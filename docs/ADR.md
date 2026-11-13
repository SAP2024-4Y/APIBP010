# Architecture Decision Record — APIBP010 Hospital Management System

## 1. Business Problem & Consumers

Hospitals struggle to coordinate patient appointments, doctor availability, and facility
locations across fragmented systems. Staff waste time resolving scheduling conflicts by
phone, and patients have no self-service way to discover available doctors or book slots.

This API solves that coordination problem. It exposes all scheduling, discovery, and
location data through a single backend service. Primary consumers are:

- **Patient-facing web and mobile apps** — search for doctors, view availability, book
  appointments.
- **Hospital staff dashboards** — manage doctor schedules, review appointment history, run
  conflict checks.
- **Third-party health aggregators** — query hospital locations and department listings
  through a stable, versioned REST interface.

## 2. Business Capabilities

The API enables five core business capabilities:

1. **Doctor discovery** — find available doctors by specialization, consultation fee range,
   and geographic proximity; navigate large result sets with stable cursor pagination.
2. **Appointment booking with conflict prevention** — create, reschedule, and cancel
   appointments; enforce working-hours constraints (09:00–17:00), 15-minute inter-appointment
   buffers, and overlap detection.
3. **Smart conflict recovery** — when a slot is unavailable, return three ready-to-book
   alternative slots in the same `409` response instead of forcing a second round-trip.
4. **Geospatial hospital search** — locate hospitals by radius (`ST_DWithin`) or
   bounding-box (`ST_Within`) using PostGIS-indexed geometry columns.
5. **Doctor availability heatmap** — surface weekly slot density as a 7×24 integer grid so
   UIs can render availability calendars without issuing 168 individual queries.

## 3. Resource Model

Five first-class business resources and their relationships:

| Resource | Key Attributes | Relates To |
|---|---|---|
| **Hospital** | name, type (GENERAL/SPECIALTY/CLINIC), location (PostGIS Point) | owns Departments |
| **Department** | name, category (specialization bucket), status | belongs to Hospital |
| **Doctor** | name, specialization, consultationFee, status (ACTIVE/ON\_LEAVE/INACTIVE), location | belongs to Department; has TimeSlots |
| **Patient** | name, dateOfBirth, contact, email | has Appointments |
| **Appointment** | startsAt, endsAt, status (CONFIRMED/CANCELLED/COMPLETED), type | links Doctor ↔ Patient |
| **TimeSlot** | slotDate, startTime, endTime, isAvailable | child of Doctor; controls heatmap grid |

Hospital and Doctor carry a PostGIS `GEOMETRY(Point, 4326)` column indexed with a GiST
index, enabling sub-millisecond spatial filtering without application-level math.

## 4. Approach Decision

We chose **Approach A: REST-First + GraphQL Layer**. REST was built first because the three
mandatory domain patterns (List, Map, Calendar) map directly and naturally onto HTTP
semantics: `GET` with query parameters for filtering, standard status codes for error
signalling, and URI versioning (`/api/v1/`) for long-lived clients.

The primary impedance mismatch between the two paradigms is HTTP caching and status codes.
REST lets `GET /api/v1/hospitals/nearby` be cached at the CDN layer by URL; the equivalent
GraphQL query is always a `POST /graphql` and cannot be HTTP-cached by default. We resolved
this by keeping all read-heavy, geospatial, and calendar queries as REST endpoints and
exposing the same data in GraphQL for clients that need flexible field selection.

The GraphQL resolvers call the same Spring service beans as the REST controllers — there is
no duplicated business logic. This single service layer guarantees that both interfaces
return identical data and enforce identical rules.

## 5. Domain Pattern Mapping

| Pattern | REST | GraphQL |
|---|---|---|
| **List** | `GET /api/v1/doctors` — filter `status`, `category`, `min_price`, `max_price`; sort `sort`/`order`; cursor paginate `limit`/`after` | `doctors(first, after, status, category, minPrice, maxPrice)` → `DoctorConnection` (Relay-spec edges + `pageInfo`) |
| **Map** | `GET /api/v1/hospitals/nearby?latitude&longitude&radius_km`, `GET /api/v1/hospitals/search?min_lat&min_lon&max_lat&max_lon` | `hospitalsNearby(latitude, longitude, radiusKm)`, `hospitalsInBox(minLat, minLon, maxLat, maxLon)` |
| **Calendar** | `GET /api/v1/doctors/{id}/appointments?starts_at&ends_at` (ISO 8601 UTC), `POST /api/v1/appointments` with conflict logic | `appointments(doctorId, startsAt, endsAt)`, mutations `bookAppointment`, `rescheduleAppointment`, `cancelAppointment` |

## 6. Composite Payload Comparison

**Scenario:** fetch a confirmed appointment with its doctor name and patient name — a common
dashboard view, measured against live endpoints on this service.

**REST — 3 round-trips:**

| Request | Bytes (approx.) |
|---|---|
| `GET /api/v1/doctors/{id}/appointments?starts_at=…&ends_at=…` | ~380 |
| `GET /api/v1/doctors/{doctorId}` | ~210 |
| `GET /api/v1/patients/{patientId}` | ~180 |
| **Total** | **~770 bytes, 3 round-trips** |

**GraphQL — 1 round-trip:**

Querying `appointments(doctorId, startsAt, endsAt) { id startsAt endsAt status doctor { name specialization } patient { name email } }` via `POST /graphql` returns only the requested fields in a single response of ~290 bytes — a 62% reduction in transferred data and three fewer network round-trips.

DataLoader batching (registered in `DataLoaderConfig`) ensures that resolving `doctor` and
`patient` across multiple appointment rows costs one `SELECT … WHERE id IN (…)` each,
not one query per row.

## 7. Error Catalogue

| Code | HTTP | GraphQL `extensions.code` | Cause |
|---|---|---|---|
| `APPOINTMENT_CONFLICT` | 409 | `APPOINTMENT_CONFLICT` | Requested slot overlaps an existing confirmed appointment |
| `SLOT_UNAVAILABLE` | 409 | `SLOT_UNAVAILABLE` | `time_slots.is_available = false` for the requested slot |
| `PATIENT_NOT_FOUND` | 404 | `PATIENT_NOT_FOUND` | Patient UUID does not exist |
| `DOCTOR_NOT_FOUND` | 404 | `DOCTOR_NOT_FOUND` | Doctor UUID does not exist |
| `HOSPITAL_NOT_FOUND` | 404 | `HOSPITAL_NOT_FOUND` | Hospital UUID does not exist |
| `INVALID_DATE_RANGE` | 400 | `INVALID_DATE_RANGE` | `ends_at` ≤ `starts_at`, or malformed ISO week string |
| `UNAUTHORIZED` | 401 | `UNAUTHORIZED` | Missing or invalid Bearer token |
| `FORBIDDEN` | 403 | `FORBIDDEN` | Valid token but insufficient role |
| `RATE_LIMITED` | 429 | `RATE_LIMITED` | Client exceeded 60 requests per minute |
| `OUTSIDE_WORKING_HOURS` | 422 | `OUTSIDE_WORKING_HOURS` | Appointment start time outside 09:00–17:00 |
| `MINIMUM_BUFFER_VIOLATION` | 422 | `MINIMUM_BUFFER_VIOLATION` | Less than 15 minutes between adjacent appointments |

`GlobalExceptionHandler` implements `DataFetcherExceptionResolverAdapter`, so a single
`throw new DomainException(ErrorCode.X, message)` anywhere in the service layer produces
the correct HTTP body for REST and the correct `errors[].extensions.code` for GraphQL.

## 8. Design Decisions

**Decision 1 — Cursor (keyset) pagination over offset pagination.**
Offset pagination drifts when rows are inserted or deleted between pages, causing duplicate
or skipped records. We encode the last seen `id|sortFieldValue` as an opaque Base64 cursor
(`CursorUtils`). The next page is fetched with a keyset `WHERE` clause that is index-backed,
making it O(log n) regardless of result-set depth. The `after` parameter in REST and the
GraphQL Connection spec `after` argument are intentionally identical so clients need only
one cursor-handling path.

**Decision 2 — PostGIS for geospatial queries instead of application-level Haversine math.**
`ST_DWithin` and `ST_Within` operate against GiST-indexed geometry columns and scale to
millions of rows without full-table scans. Performing radius math in Java would require
fetching all hospitals, running Haversine per row, and filtering in memory — correct but
unscalable. Moving the spatial predicate into the database query also lets the planner
combine the spatial filter with status filters in a single pass.

**Decision 3 — DataLoader batching to prevent N+1 in GraphQL resolvers.**
When the `doctors` query returns 20 results, a naïve `@SchemaMapping` on `Doctor.department`
would fire 20 individual `SELECT * FROM departments WHERE id = ?` calls. `DepartmentDataLoader`
and `DoctorDataLoader` accumulate all requested IDs within a single execution tick and
resolve them with one `SELECT … WHERE id IN (…)`, reducing 20 queries to 1.

## 9. Future Evolution

1. **API v2 with appointment type expansion.** Add `TELEMEDICINE` as a first-class
   appointment type with a video-link field and a separate conflict rule (no physical-room
   buffer required). The existing `/api/v1/` prefix means v2 can be introduced at
   `/api/v2/appointments` without breaking existing clients. The GraphQL schema already has
   `doctorsBySpecialty` marked `@deprecated` as a worked example of non-breaking field
   evolution.

2. **Real-time availability via GraphQL Subscriptions.** When a slot is booked or cancelled,
   push an update to connected clients rather than requiring them to poll
   `/doctors/{id}/heatmap`. Spring for GraphQL supports WebSocket-backed subscriptions
   without a framework change.

3. **Multi-tenant hospital network support.** Currently all hospitals share one schema.
   Adding a `network_id` foreign key to `hospitals` and a corresponding JWT `network` claim
   would allow a single deployment to serve multiple independent hospital groups with
   row-level data isolation, enabling SaaS operation without a separate instance per tenant.

## 10. Business Value

1. **Reduced scheduling friction and no-shows.** The Smart Appointment Suggester returns
   three immediately bookable alternatives inside the `409` conflict response, eliminating
   the back-and-forth of finding a new slot. Fewer failed booking attempts means more
   confirmed appointments and lower administrative overhead.

2. **Operational visibility through the heatmap.** The `GET /api/v1/doctors/{id}/heatmap`
   endpoint surfaces a doctor's weekly availability as a 7×24 grid in a single call.
   Hospital operations teams can identify under-utilised doctors and rebalance clinic
   schedules without writing database queries, reducing slot wastage.

3. **Developer ecosystem flexibility.** Exposing both REST and GraphQL from the same service
   allows REST-native integrations (third-party aggregators, insurance portals) to coexist
   with richer client applications that need composite queries. Neither audience forces a
   compromise on the other, and the shared service layer guarantees identical business rules
   across both interfaces.

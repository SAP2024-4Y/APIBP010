-- V2: Seed hospitals (24 total)
--
-- Spatial layout verified from centre (77.2090, 28.6139):
--   5km  radius → 3 results  (001, 002, 003 — Delhi cluster)
--   50km radius → 6 results  (001–006; 007 is INACTIVE, excluded by nearby query)
--   All other cities are >230 km away — safe from NCR radius tests
--
-- WHY staggered created_at?
--   V3 links all 5 departments to: SELECT id FROM hospitals ORDER BY created_at LIMIT 1
--   A single-batch insert gives all rows the same NOW() timestamp, making ORDER BY
--   non-deterministic. Staggering guarantees b1000000-...000001 (AIIMS Delhi) always wins.
--
-- WHY POINT(longitude latitude)?
--   PostGIS WKT uses (X Y) = (lon lat). Matches GeoJsonUtils.toGeoJson() → [getX(), getY()].

INSERT INTO hospitals (id, name, address, type, status, location, created_at) VALUES

  -- ── Delhi cluster: 3 hospitals within 5 km of (77.2090, 28.6139) ──────────

  -- 0.0 km — first by created_at, so V3 departments always link here
  ('b1000000-0000-0000-0000-000000000001',
   'AIIMS Delhi',
   'Sri Aurobindo Marg, Ansari Nagar, New Delhi 110029',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(77.2090 28.6139)', 4326),
   '2026-01-01 00:00:00+00'),

  -- 2.0 km south
  ('b1000000-0000-0000-0000-000000000002',
   'Safdarjung Hospital Delhi',
   'Sri Aurobindo Marg, New Delhi 110029',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(77.2060 28.5960)', 4326),
   '2026-01-01 00:00:01+00'),

  -- 3.0 km north-east
  ('b1000000-0000-0000-0000-000000000003',
   'RML Hospital Delhi',
   'Baba Kharak Singh Marg, New Delhi 110001',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(77.2310 28.6330)', 4326),
   '2026-01-01 00:00:02+00'),

  -- ── Delhi NCR: within 50 km, outside 5 km ────────────────────────────────

  -- 9.9 km south
  ('b1000000-0000-0000-0000-000000000004',
   'Max Super Speciality Hospital Saket',
   '1, 2, Press Enclave Road, Saket, New Delhi 110017',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(77.2190 28.5250)', 4326),
   '2026-01-01 00:00:03+00'),

  -- 18.4 km east (Noida)
  ('b1000000-0000-0000-0000-000000000005',
   'Apollo Hospital Noida',
   'E-2, Sector 26, Noida 201301',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(77.3910 28.5700)', 4326),
   '2026-01-01 00:00:04+00'),

  -- 24.7 km south-west (Gurugram)
  ('b1000000-0000-0000-0000-000000000006',
   'Medanta The Medicity',
   'CH Baktawar Singh Road, Sector 38, Gurugram 122001',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(77.0266 28.4595)', 4326),
   '2026-01-01 00:00:05+00'),

  -- 26.2 km south-east (Faridabad) — INACTIVE: appears in list but excluded from nearby
  ('b1000000-0000-0000-0000-000000000007',
   'Fortis Escorts Faridabad',
   'Neelam Bata Road, NIT, Faridabad 121001',
   'GENERAL', 'INACTIVE',
   ST_GeomFromText('POINT(77.3310 28.4040)', 4326),
   '2026-01-01 00:00:06+00'),

  -- ── Mumbai: 4 hospitals ───────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000008',
   'Apollo Hospitals Mumbai',
   'Plot 13, Parsee Colony, Juhu, Mumbai 400049',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(72.8268 19.1197)', 4326),
   '2026-01-01 00:00:07+00'),

  ('b1000000-0000-0000-0000-000000000009',
   'Kokilaben Dhirubhai Ambani Hospital',
   'Rao Saheb Achutrao Patwardhan Marg, Mumbai 400053',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(72.8271 19.1338)', 4326),
   '2026-01-01 00:00:08+00'),

  ('b1000000-0000-0000-0000-000000000015',
   'Fortis Hospital Mulund',
   'Mulund Goregaon Link Road, Mulund West, Mumbai 400078',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(72.9580 19.1720)', 4326),
   '2026-01-01 00:00:09+00'),

  ('b1000000-0000-0000-0000-000000000016',
   'Lilavati Hospital Mumbai',
   'A-791, Bandra Reclamation, Bandra West, Mumbai 400050',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(72.8273 19.0528)', 4326),
   '2026-01-01 00:00:10+00'),

  -- ── Bangalore: 3 hospitals ────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000010',
   'Manipal Hospital Bangalore',
   '98, HAL Airport Road, Bangalore 560017',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(77.6488 12.9698)', 4326),
   '2026-01-01 00:00:11+00'),

  ('b1000000-0000-0000-0000-000000000011',
   'Narayana Health City Bangalore',
   '258/A, Bommasandra Industrial Area, Bangalore 560099',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(77.7017 12.8125)', 4326),
   '2026-01-01 00:00:12+00'),

  ('b1000000-0000-0000-0000-000000000017',
   'Cloudnine Hospital Bangalore',
   'No. 1533, 16th Cross, Sadashivanagar, Bangalore 560080',
   'CLINIC', 'ACTIVE',
   ST_GeomFromText('POINT(77.5771 12.9716)', 4326),
   '2026-01-01 00:00:13+00'),

  -- ── Kolkata: 2 hospitals ──────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000018',
   'SSKM Hospital Kolkata',
   '244, Acharya J.C. Bose Road, Kolkata 700020',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(88.3483 22.5356)', 4326),
   '2026-01-01 00:00:14+00'),

  ('b1000000-0000-0000-0000-000000000019',
   'Apollo Gleneagles Hospital Kolkata',
   '58, Canal Circular Road, Kadapara, Kolkata 700054',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(88.4022 22.5448)', 4326),
   '2026-01-01 00:00:15+00'),

  -- ── Chennai: 1 hospital ───────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000012',
   'Sri Ramachandra Medical Centre',
   'No.1, Ramachandra Nagar, Porur, Chennai 600116',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(80.1547 13.0350)', 4326),
   '2026-01-01 00:00:16+00'),

  -- ── Hyderabad: 1 hospital ─────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000013',
   'KIMS Hospitals Hyderabad',
   '1-8-31/1, Minister Road, Secunderabad 500003',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(78.4649 17.4314)', 4326),
   '2026-01-01 00:00:17+00'),

  -- ── Pune: 1 hospital ──────────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000014',
   'Ruby Hall Clinic Pune',
   '40, Sassoon Road, Pune 411001',
   'CLINIC', 'ACTIVE',
   ST_GeomFromText('POINT(73.8946 18.5273)', 4326),
   '2026-01-01 00:00:18+00'),

  -- ── Jaipur: 1 hospital ───────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000020',
   'Sawai Man Singh Hospital Jaipur',
   'Sawai Ram Singh Road, Jaipur 302004',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(75.8052 26.9124)', 4326),
   '2026-01-01 00:00:19+00'),

  -- ── Ahmedabad: 1 hospital ─────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000021',
   'Sterling Hospital Ahmedabad',
   'Off Gurukul Road, Memnagar, Ahmedabad 380052',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(72.5292 23.0395)', 4326),
   '2026-01-01 00:00:20+00'),

  -- ── Lucknow: 1 hospital ───────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000022',
   'Medanta Hospital Lucknow',
   'Sector A, Pocket 1, Amar Shaheed Path, Lucknow 226030',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(80.9462 26.8467)', 4326),
   '2026-01-01 00:00:21+00'),

  -- ── Kochi: 1 hospital ─────────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000023',
   'Amrita Institute of Medical Sciences Kochi',
   'Ponekkara, Edappally, Kochi 682041',
   'SPECIALTY', 'ACTIVE',
   ST_GeomFromText('POINT(76.2699 10.0261)', 4326),
   '2026-01-01 00:00:22+00'),

  -- ── Chandigarh: 1 hospital ────────────────────────────────────────────────

  ('b1000000-0000-0000-0000-000000000024',
   'PGIMER Chandigarh',
   'Sector 12, Chandigarh 160012',
   'GENERAL', 'ACTIVE',
   ST_GeomFromText('POINT(76.7794 30.7333)', 4326),
   '2026-01-01 00:00:23+00');

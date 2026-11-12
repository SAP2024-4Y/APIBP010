CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE hospitals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    address     TEXT,
    type        VARCHAR(50)  NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    location    GEOMETRY(Point, 4326),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE departments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    hospital_id UUID         NOT NULL REFERENCES hospitals(id),
    category    VARCHAR(100) NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE doctors (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    specialization    VARCHAR(100) NOT NULL,
    consultation_fee  NUMERIC(10, 2) NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    department_id     UUID         REFERENCES departments(id),
    location          GEOMETRY(Point, 4326),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE patients (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    date_of_birth DATE,
    contact       VARCHAR(20),
    email         VARCHAR(200),
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE time_slots (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id    UUID    NOT NULL REFERENCES doctors(id),
    slot_date    DATE    NOT NULL,
    start_time   TIME    NOT NULL,
    end_time     TIME    NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE appointments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id  UUID        NOT NULL REFERENCES doctors(id),
    patient_id UUID        NOT NULL REFERENCES patients(id),
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    type       VARCHAR(50) NOT NULL DEFAULT 'CONSULTATION',
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospitals_location    ON hospitals    USING GIST(location);
CREATE INDEX idx_doctors_location      ON doctors      USING GIST(location);
CREATE INDEX idx_appointments_doc_time ON appointments (doctor_id, starts_at, ends_at);
CREATE INDEX idx_time_slots_doc_date   ON time_slots   (doctor_id, slot_date);

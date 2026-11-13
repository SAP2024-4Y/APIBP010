-- V4: Seed patients and time slots
-- Time slots: Mon-Fri 09:00-17:00 in 30-min increments for December 2026
-- Uses doctor IDs from V3

-- ── Patients ────────────────────────────────────────────────────────────────
INSERT INTO patients (id, name, date_of_birth, contact, email, status) VALUES
  ('b1000000-0000-0000-0000-000000000001', 'Ananya Krishnan',   '1990-03-15', '9876543201', 'ananya.krishnan@email.com',   'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000002', 'Rahul Sharma',      '1985-07-22', '9876543202', 'rahul.sharma@email.com',      'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000003', 'Priya Patel',       '1992-11-08', '9876543203', 'priya.patel@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000004', 'Vikram Reddy',      '1978-05-30', '9876543204', 'vikram.reddy@email.com',      'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000005', 'Sonia Gupta',       '1995-09-14', '9876543205', 'sonia.gupta@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000006', 'Arjun Nair',        '1983-02-28', '9876543206', 'arjun.nair@email.com',        'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000007', 'Meena Iyer',        '1970-12-05', '9876543207', 'meena.iyer@email.com',        'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000008', 'Suresh Kumar',      '1965-08-19', '9876543208', 'suresh.kumar@email.com',      'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000009', 'Deepa Menon',       '1998-04-25', '9876543209', 'deepa.menon@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000010', 'Karthik Bose',      '1987-06-11', '9876543210', 'karthik.bose@email.com',      'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000011', 'Lakshmi Rao',       '1993-01-17', '9876543211', 'lakshmi.rao@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000012', 'Mohan Das',         '1975-10-03', '9876543212', 'mohan.das@email.com',         'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000013', 'Nisha Singh',       '1989-07-27', '9876543213', 'nisha.singh@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000014', 'Ravi Tiwari',       '1982-03-09', '9876543214', 'ravi.tiwari@email.com',       'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000015', 'Sunita Joshi',      '1996-11-21', '9876543215', 'sunita.joshi@email.com',      'ACTIVE'),
  ('b1000000-0000-0000-0000-000000000016', 'Arun Pillai',       '1971-06-16', '9876543216', 'arun.pillai@email.com',       'INACTIVE');

-- ── Time Slots ───────────────────────────────────────────────────────────────
-- For each of the 20 doctors, generate Mon-Fri 09:00-17:00 in 30-min slots for Dec 2026
-- 16 slots per day (09:00 to 17:00 = 16 half-hours), Mon-Fri
INSERT INTO time_slots (id, doctor_id, slot_date, start_time, end_time, is_available)
SELECT
    gen_random_uuid(),
    d.id,
    gs::date,
    (INTERVAL '9 hours' + (n * INTERVAL '30 minutes'))::time,
    (INTERVAL '9 hours' + ((n + 1) * INTERVAL '30 minutes'))::time,
    TRUE
FROM doctors d,
     generate_series('2026-12-01'::date, '2026-12-31'::date, '1 day') gs,
     generate_series(0, 15) n
WHERE EXTRACT(DOW FROM gs) BETWEEN 1 AND 5;

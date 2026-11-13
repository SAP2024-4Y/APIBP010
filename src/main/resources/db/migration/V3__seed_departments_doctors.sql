-- V3: Seed departments and doctors
-- Requires V2 (hospitals) to exist; links departments to the first hospital in the table

-- ── Departments ────────────────────────────────────────────────────────────────
INSERT INTO departments (id, name, hospital_id, category, status) VALUES
  ('a1000000-0000-0000-0000-000000000001', 'Cardiology Dept',       (SELECT id FROM hospitals ORDER BY created_at LIMIT 1), 'CARDIOLOGY',       'ACTIVE'),
  ('a1000000-0000-0000-0000-000000000002', 'Neurology Dept',         (SELECT id FROM hospitals ORDER BY created_at LIMIT 1), 'NEUROLOGY',         'ACTIVE'),
  ('a1000000-0000-0000-0000-000000000003', 'Orthopedics Dept',       (SELECT id FROM hospitals ORDER BY created_at LIMIT 1), 'ORTHOPEDICS',       'ACTIVE'),
  ('a1000000-0000-0000-0000-000000000004', 'Pediatrics Dept',        (SELECT id FROM hospitals ORDER BY created_at LIMIT 1), 'PEDIATRICS',        'ACTIVE'),
  ('a1000000-0000-0000-0000-000000000005', 'General Medicine Dept',  (SELECT id FROM hospitals ORDER BY created_at LIMIT 1), 'GENERAL_MEDICINE',  'ACTIVE');

-- ── Doctors ────────────────────────────────────────────────────────────────────
INSERT INTO doctors (id, name, specialization, consultation_fee, status, department_id, location) VALUES

  -- CARDIOLOGY (4 doctors)
  ('d1000000-0000-0000-0000-000000000001', 'Dr. Priya Sharma',    'CARDIOLOGY', 1200.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000001', ST_GeomFromText('POINT(77.2090 28.6139)', 4326)),  -- Delhi centre

  ('d1000000-0000-0000-0000-000000000002', 'Dr. Arjun Mehta',     'CARDIOLOGY',  800.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000001', ST_GeomFromText('POINT(72.8777 19.0760)', 4326)),  -- Mumbai

  ('d1000000-0000-0000-0000-000000000003', 'Dr. Neha Kapoor',     'CARDIOLOGY', 1500.00, 'ON_LEAVE',
   'a1000000-0000-0000-0000-000000000001', ST_GeomFromText('POINT(77.5946 12.9716)', 4326)),  -- Bangalore

  ('d1000000-0000-0000-0000-000000000004', 'Dr. Rajesh Verma',    'CARDIOLOGY',  500.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000001', ST_GeomFromText('POINT(77.2300 28.6200)', 4326)),  -- Delhi near

  -- NEUROLOGY (4 doctors)
  ('d1000000-0000-0000-0000-000000000005', 'Dr. Sunita Iyer',     'NEUROLOGY',  1800.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000002', ST_GeomFromText('POINT(80.2707 13.0827)', 4326)),  -- Chennai

  ('d1000000-0000-0000-0000-000000000006', 'Dr. Vikram Rao',      'NEUROLOGY',   900.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000002', ST_GeomFromText('POINT(78.4867 17.3850)', 4326)),  -- Hyderabad

  ('d1000000-0000-0000-0000-000000000007', 'Dr. Ananya Das',      'NEUROLOGY',  1100.00, 'ON_LEAVE',
   'a1000000-0000-0000-0000-000000000002', ST_GeomFromText('POINT(73.8567 18.5204)', 4326)),  -- Pune

  ('d1000000-0000-0000-0000-000000000008', 'Dr. Karan Singh',     'NEUROLOGY',   600.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000002', ST_GeomFromText('POINT(77.2000 28.6000)', 4326)),  -- Delhi

  -- ORTHOPEDICS (4 doctors)
  ('d1000000-0000-0000-0000-000000000009', 'Dr. Meera Pillai',    'ORTHOPEDICS', 2000.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000003', ST_GeomFromText('POINT(72.8656 19.0822)', 4326)),  -- Mumbai

  ('d1000000-0000-0000-0000-000000000010', 'Dr. Rohit Joshi',     'ORTHOPEDICS',  700.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000003', ST_GeomFromText('POINT(77.6100 12.9800)', 4326)),  -- Bangalore

  ('d1000000-0000-0000-0000-000000000011', 'Dr. Preethi Nair',    'ORTHOPEDICS',  950.00, 'INACTIVE',
   'a1000000-0000-0000-0000-000000000003', ST_GeomFromText('POINT(80.2500 13.0700)', 4326)),  -- Chennai

  ('d1000000-0000-0000-0000-000000000012', 'Dr. Sameer Khan',     'ORTHOPEDICS', 1300.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000003', ST_GeomFromText('POINT(77.2090 28.5500)', 4326)),  -- Delhi south

  -- PEDIATRICS (4 doctors)
  ('d1000000-0000-0000-0000-000000000013', 'Dr. Divya Reddy',     'PEDIATRICS',   400.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000004', ST_GeomFromText('POINT(78.5000 17.4000)', 4326)),  -- Hyderabad

  ('d1000000-0000-0000-0000-000000000014', 'Dr. Aakash Gupta',    'PEDIATRICS',   300.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000004', ST_GeomFromText('POINT(73.8700 18.5300)', 4326)),  -- Pune

  ('d1000000-0000-0000-0000-000000000015', 'Dr. Pooja Sharma',    'PEDIATRICS',   650.00, 'ON_LEAVE',
   'a1000000-0000-0000-0000-000000000004', ST_GeomFromText('POINT(77.5800 12.9600)', 4326)),  -- Bangalore

  ('d1000000-0000-0000-0000-000000000016', 'Dr. Tushar Patil',    'PEDIATRICS',   850.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000004', ST_GeomFromText('POINT(72.8900 19.1000)', 4326)),  -- Mumbai

  -- GENERAL_MEDICINE (4 doctors)
  ('d1000000-0000-0000-0000-000000000017', 'Dr. Kavya Menon',     'GENERAL_MEDICINE',  350.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000005', ST_GeomFromText('POINT(77.2100 28.6250)', 4326)),  -- Delhi

  ('d1000000-0000-0000-0000-000000000018', 'Dr. Nikhil Bose',     'GENERAL_MEDICINE', 1000.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000005', ST_GeomFromText('POINT(80.2800 13.0900)', 4326)),  -- Chennai

  ('d1000000-0000-0000-0000-000000000019', 'Dr. Shreya Agarwal',  'GENERAL_MEDICINE',  450.00, 'INACTIVE',
   'a1000000-0000-0000-0000-000000000005', ST_GeomFromText('POINT(72.8600 19.0700)', 4326)),  -- Mumbai

  ('d1000000-0000-0000-0000-000000000020', 'Dr. Amitabh Tiwari',  'GENERAL_MEDICINE', 1600.00, 'ACTIVE',
   'a1000000-0000-0000-0000-000000000005', ST_GeomFromText('POINT(77.6200 12.9900)', 4326)); -- Bangalore

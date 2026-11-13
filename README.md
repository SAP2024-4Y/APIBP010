# APIBP001 — Hospital Management System

Course: SESAPZG504 - API Based Products | Section: 2024 4Y | Instructor: Vinaya Sathyanarayan

## Team Details

| #   | Name             | Roll No     | GitHub ID     | Email                              |
| --- | ---------------- | ----------- | ------------- | ---------------------------------- |
| 1   | Aditya Sah       | 2024SL70016 | AdityaSah4    | 2024sl70016@wilp.bits-pilani.ac.in |
| 2   | Ahad Abbas Rizvi | 2024SL70035 | Atteraver     | 2024sl70035@wilp.bits-pilani.ac.in |
| 3   | Rohit Negi       | 2024SL70028 | Rohit-089     | 2024sl70028@wilp.bits-pilani.ac.in |
| 4   | Saanvi Singh     | 2024SL70037 | SaanviSingh12 | 2024sl70037@wilp.bits-pilani.ac.in |
| 5   | Shivodit Gill    | 2024SL70040 | HotDogEnemy   | 2024sl70040@wilp.bits-pilani.ac.in |

- Team ID: `APIBP010`
- Chosen Approach: `A` — REST-First + GraphQL Layer

## Quick start

```bash
git clone https://github.com/SAP2024-4Y/APIBP010
cd APIBP010
docker compose up
# Server:      http://localhost:8080
# Health:      http://localhost:8080/health
# REST docs:   http://localhost:8080/docs
# GraphQL IDE: http://localhost:8080/graphql
```

## Demo credentials

```
POST /api/v1/auth/token  { "username": "demo", "password": "demo-password" }
```

## Run tests

```bash
# Unit tests (no DB, fast)
docker compose --profile test run --rm test

# Integration tests (Testcontainers spins up its own PostgreSQL via Docker socket)
docker compose --profile test run --rm test \
  mvn --batch-mode verify -Dgroups=integration -DskipUnitTests=true
```

## Sample REST call

```bash
curl -H "Authorization: Bearer <token>" \
     "http://localhost:8080/api/v1/<your-resource>?limit=10&after=<cursor>&status=<x>&sort=id&order=asc"
```

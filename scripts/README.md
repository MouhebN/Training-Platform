# Scripts

SQL helpers live here for one-off migrations / optional SQL seed.

**Preferred seed:** Spring `DataSeeder` in `backend/` runs automatically on startup when demo users are missing.

Optional SQL seed (if you still want it):

```bash
./scripts/seed-demo-data.sh
```

Requires Postgres reachable and Hibernate tables already created.

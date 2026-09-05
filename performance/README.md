# Performance testing

The Gatling simulation validates the assumed workload of 200 read requests and
100 write requests per second. A write iteration performs one `POST` and one
`PATCH`, so Gatling starts 50 write iterations per second at the default load.

## Prepare the environment

The performance environment is isolated in `performance/compose.yml`. It uses
port `18080`, a dedicated PostgreSQL volume, and explicit CPU and memory limits.
The helper script starts this environment automatically.

To start it manually:

```bash
docker compose -f performance/compose.yml up --build -d postgres api
```

For the full load profile, the helper script automatically runs the `seed-data`
service after the API is ready and Liquibase has created the schema. The seed
script brings the table up to one million rows instead of appending another
million on every run. Before Gatling starts, the helper prints the actual
number of rows in `devices`, for example `Device records before Gatling: 1000000`.

To run the seed manually:

```bash
docker compose -f performance/compose.yml \
  --profile seed run --rm seed-data
```

The smoke profile skips seeding by default. Override either behavior with
`SEED_DATA=true` or `SEED_DATA=false`.

## Run the target load

Use the helper script to start the Compose environment, wait for readiness, and
run the simulation:

```bash
./performance/run-gatling.sh
```

The default profile ramps up for 60 seconds and then holds the target load for
10 minutes. The run fails when successful requests are not above 99.9% or the
95th-percentile response time reaches one second.

Generated HTML reports are written under `target/gatling`.

## Run a short smoke test

```bash
PERF_PROFILE=smoke ./performance/run-gatling.sh
```

When the test finishes, fails, or is interrupted, the script stops the Compose
stack that it started. The dedicated database volume is preserved between runs.
Before stopping the stack, service logs are saved to
`target/performance/compose.log` for failure analysis.
To stop a manually started performance environment:

```bash
docker compose -f performance/compose.yml down
```

Delete its test data only when a clean performance database is required:

```bash
docker compose -f performance/compose.yml down -v
```

To test an already running local or remote instance without starting Compose:

```bash
START_STACK=false \
  PERF_BASE_URL=https://staging.example.com \
  ./performance/run-gatling.sh
```

## Configuration

| Property | Default | Description |
|---|---:|---|
| `perf.baseUrl` | `http://localhost:18080` | Target service URL |
| `perf.readRps` | `200` | Read requests per second |
| `perf.writeRps` | `100` | Combined POST and PATCH requests per second |
| `perf.rampSeconds` | `60` | Ramp-up duration |
| `perf.durationSeconds` | `600` | Steady-load duration |

The helper script exposes the same settings as environment variables:
`PERF_BASE_URL`, `PERF_READ_RPS`, `PERF_WRITE_RPS`, `PERF_RAMP_SECONDS`, and
`PERF_DURATION_SECONDS`. `PERF_API_PORT` changes the local published port,
`SEED_DATA` controls database seeding, and `START_STACK=false` prevents the
script from starting the local Compose environment.

Run the load generator separately from the service for trustworthy production
measurements, and record the CPU, memory, JVM, HikariCP, and PostgreSQL metrics
alongside the Gatling report.

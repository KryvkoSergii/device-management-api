#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(dirname "$SCRIPT_DIR")

PERF_PROFILE=${PERF_PROFILE:-load}
PERF_API_PORT=${PERF_API_PORT:-18080}
PERF_BASE_URL=${PERF_BASE_URL:-http://localhost:${PERF_API_PORT}}
START_STACK=${START_STACK:-true}
COMPOSE_FILE=${COMPOSE_FILE:-performance/compose.yml}
READINESS_URL="${PERF_BASE_URL}/actuator/health/readiness"
STACK_STARTED=false

cleanup() {
  exit_code=$?
  trap - 0 HUP INT TERM

  if [ "$STACK_STARTED" = "true" ]; then
    mkdir -p target/performance
    echo "Saving Docker Compose logs to target/performance/compose.log..."
    docker compose -f "$COMPOSE_FILE" logs --no-color \
      > target/performance/compose.log 2>&1 || true

    echo "Stopping the performance Docker Compose stack..."
    docker compose -f "$COMPOSE_FILE" down || true
  fi

  exit "$exit_code"
}

trap cleanup 0 HUP INT TERM

if [ "${SEED_DATA+x}" != "x" ]; then
  if [ "$PERF_PROFILE" = "load" ] && [ "$START_STACK" = "true" ]; then
    SEED_DATA=true
  else
    SEED_DATA=false
  fi
fi

case "$PERF_PROFILE" in
  smoke|load)
    ;;
  *)
    echo "Unsupported PERF_PROFILE: $PERF_PROFILE (expected: smoke or load)" >&2
    exit 2
    ;;
esac

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to check application readiness" >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [ "$START_STACK" = "true" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required when START_STACK=true" >&2
    exit 1
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running" >&2
    exit 1
  fi

  echo "Starting the API and PostgreSQL..."
  export PERF_API_PORT
  STACK_STARTED=true
  docker compose -f "$COMPOSE_FILE" up --build -d postgres api
fi

echo "Waiting for application readiness at $READINESS_URL..."
attempt=1
max_attempts=60

while ! curl -fsS "$READINESS_URL" >/dev/null 2>&1; do
  if [ "$attempt" -ge "$max_attempts" ]; then
    echo "Application did not become ready within 120 seconds" >&2
    echo "Inspect logs with: docker compose logs api" >&2
    exit 1
  fi

  sleep 2
  attempt=$((attempt + 1))
done

if [ "$SEED_DATA" = "true" ]; then
  echo "Ensuring that the performance database contains one million devices..."
  docker compose -f "$COMPOSE_FILE" --profile seed run --rm seed-data
fi

if [ "$START_STACK" = "true" ]; then
  echo "Verifying the performance database size..."
  docker compose -f "$COMPOSE_FILE" exec -T postgres \
    psql --username=devices --dbname=devices --tuples-only --no-align \
    --command="SELECT 'Device records before Gatling: ' || COUNT(*) FROM devices;"
fi

echo "Application is ready. Running the $PERF_PROFILE Gatling profile..."

if [ "$PERF_PROFILE" = "smoke" ]; then
  ./mvnw -Pperformance gatling:test \
    -Dperf.baseUrl="$PERF_BASE_URL" \
    -Dperf.readRps="${PERF_READ_RPS:-2}" \
    -Dperf.writeRps="${PERF_WRITE_RPS:-2}" \
    -Dperf.rampSeconds="${PERF_RAMP_SECONDS:-1}" \
    -Dperf.durationSeconds="${PERF_DURATION_SECONDS:-10}"
  exit 0
fi

./mvnw -Pperformance gatling:test \
  -Dperf.baseUrl="$PERF_BASE_URL" \
  -Dperf.readRps="${PERF_READ_RPS:-200}" \
  -Dperf.writeRps="${PERF_WRITE_RPS:-100}" \
  -Dperf.rampSeconds="${PERF_RAMP_SECONDS:-60}" \
  -Dperf.durationSeconds="${PERF_DURATION_SECONDS:-600}"

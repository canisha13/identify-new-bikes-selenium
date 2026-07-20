#!/usr/bin/env bash
###############################################################################
# run-tests.sh
#
# Automated Selenium test suite runner for: identify-new-bikes-selenium
#
# Mirrors the existing Jenkinsfile stages so the exact same suite can be run
# locally (Git Bash / WSL) or inside Jenkins:
#   clean reports -> build test image -> start Selenium Grid -> wait for grid
#   -> run tests -> generate reports (JUnit + Allure) -> teardown grid
###############################################################################

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP="$(date +'%Y-%m-%d_%H-%M-%S')"

TARGET_DIR="${PROJECT_ROOT}/target"
LOG_DIR="${TARGET_DIR}/logs"
LOG_FILE="${LOG_DIR}/run-${TIMESTAMP}.log"
ALLURE_RESULTS_DIR="${TARGET_DIR}/allure-results"
SUREFIRE_DIR="${TARGET_DIR}/surefire-reports"
SCREENSHOTS_DIR="${TARGET_DIR}/screenshots"
EXTENT_DIR="${TARGET_DIR}/extent"
ALLURE_REPORT_DIR="${PROJECT_ROOT}/allure-report"

OVERALL_EXIT_CODE=0

mkdir -p "$LOG_DIR"

log() {
  local level="$1"; shift
  local ts
  ts="$(date +'%Y-%m-%d %H:%M:%S')"
  echo "[${ts}] [${level}] $*" | tee -a "$LOG_FILE"
}
log_info()  { log "INFO"  "$@"; }
log_warn()  { log "WARN"  "$@"; }
log_error() { log "ERROR" "$@"; }

run_stage() {
  local stage_name="$1"; shift
  log_info "=== START STAGE: ${stage_name} ==="
  local start_ts end_ts status
  start_ts=$(date +%s)

  "$@" >> "$LOG_FILE" 2>&1
  status=$?

  end_ts=$(date +%s)
  if [ $status -eq 0 ]; then
    log_info "=== STAGE PASSED: ${stage_name} ($(( end_ts - start_ts ))s) ==="
  else
    log_error "=== STAGE FAILED: ${stage_name} (exit code ${status}, $(( end_ts - start_ts ))s) ==="
    OVERALL_EXIT_CODE=1
  fi
  return $status
}

setup_env() {
  export SELENIUM_HUB_URL="${SELENIUM_HUB_URL:-http://localhost:4444/wd/hub}"
  export BROWSER="${BROWSER:-chrome}"
  export CI="${CI:-false}"
  log_info "Environment: SELENIUM_HUB_URL=${SELENIUM_HUB_URL} BROWSER=${BROWSER} CI=${CI}"
}

install_dependencies() {
  log_info "Checking required tools (docker, docker compose)..."
  for tool in docker; do
    if ! command -v "$tool" &> /dev/null; then
      log_error "Required tool not found: ${tool}"
      return 1
    fi
  done
  if ! docker compose version &> /dev/null; then
    log_error "'docker compose' plugin not available."
    return 1
  fi

  log_info "Verifying Docker daemon is reachable..."
  docker info > /dev/null 2>&1 || {
    log_error "Cannot reach Docker daemon (permission denied or daemon not running)."
    return 1
  }

  log_info "Building test image via docker compose..."
  (cd "$PROJECT_ROOT" && docker compose build tests)
}

clean_reports() {
  log_info "Cleaning previous target/ reports..."
  rm -rf "$TARGET_DIR"
  mkdir -p "$EXTENT_DIR" "$ALLURE_RESULTS_DIR" "$SUREFIRE_DIR" "$SCREENSHOTS_DIR" "$LOG_DIR"
}

start_services() {
  log_info "Starting Selenium Grid (hub + chrome + firefox + edge)..."
  (cd "$PROJECT_ROOT" && docker compose up -d selenium-hub chrome firefox edge) || return 1

  log_info "Waiting for Selenium Grid to report ready..."
  local retries=30
  until curl -s "http://localhost:4444/wd/hub/status" 2>/dev/null | grep -q '"ready":true'; do
    retries=$((retries - 1))
    if [ $retries -le 0 ]; then
      log_error "Selenium Grid did not become ready in time."
      return 1
    fi
    sleep 2
  done
  log_info "Selenium Grid is ready."
}

stop_services() {
  log_info "Tearing down Selenium Grid containers (docker compose down -v)..."
  (cd "$PROJECT_ROOT" && docker compose down -v) >> "$LOG_FILE" 2>&1
}

run_selenium_tests() {
  log_info "Running Selenium test suite in the 'tests' container..."
  (cd "$PROJECT_ROOT" && docker compose run --rm tests)
  local status=$?

  log_info "Copying reports out of the tests container (if it still exists)..."
  local container_id
  container_id="$(cd "$PROJECT_ROOT" && docker compose ps -a -q tests)"
  if [ -n "$container_id" ]; then
    docker cp "${container_id}:/app/target/." "$TARGET_DIR/" 2>/dev/null || \
      log_warn "Could not copy reports from container ${container_id}."
  else
    log_warn "No 'tests' container found to copy reports from."
  fi

  return $status
}

generate_reports() {
  log_info "JUnit XML reports available at: ${SUREFIRE_DIR}"
  log_info "Extent HTML report available at: ${EXTENT_DIR}"

  if command -v allure &> /dev/null; then
    log_info "Generating Allure report..."
    allure generate "$ALLURE_RESULTS_DIR" -c -o "$ALLURE_REPORT_DIR" || \
      log_warn "Allure report generation failed (non-fatal)."
  else
    log_warn "Allure CLI not found on PATH - skipping Allure report generation."
    log_warn "(Jenkins has its own Allure tool install; this only matters for local runs.)"
  fi
}

main() {
  log_info "############################################################"
  log_info "  identify-new-bikes-selenium - Selenium test suite"
  log_info "  Run timestamp: ${TIMESTAMP}"
  log_info "############################################################"

  setup_env

  run_stage "Install Dependencies / Build Test Image" install_dependencies || {
    log_error "Dependency check or image build failed. Aborting - cannot continue safely."
    exit 1
  }

  run_stage "Clean Old Reports" clean_reports

  run_stage "Start Selenium Grid" start_services || {
    log_error "Selenium Grid failed to start. Skipping test execution."
  }

  run_stage "Run Selenium Tests" run_selenium_tests
  run_stage "Generate Reports"   generate_reports

  stop_services

  log_info "############################################################"
  if [ $OVERALL_EXIT_CODE -eq 0 ]; then
    log_info "  ALL STAGES PASSED"
  else
    log_error " ONE OR MORE STAGES FAILED - see ${LOG_FILE}"
  fi
  log_info "############################################################"

  exit $OVERALL_EXIT_CODE
}

trap 'log_error "Script interrupted unexpectedly."; stop_services; exit 1' INT TERM

main "$@"
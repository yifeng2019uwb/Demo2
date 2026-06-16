import os
import pytest
import requests
from datetime import datetime, timedelta, timezone

BASE_URL = os.getenv("BASE_URL", "https://report-app-88qqj.ondigitalocean.app")
API = f"{BASE_URL}/api/v1/reports"


def ts(delta_minutes=0):
    return (datetime.now(timezone.utc) - timedelta(minutes=delta_minutes)).strftime('%Y-%m-%dT%H:%M:%SZ')


@pytest.fixture(scope="session")
def created_report_id():
    payload = {
        "container_id": "container-py-test",
        "app_name": "billing-service",
        "cpu_usage_percent": 72.5,
        "memory_usage_mb": 512,
        "reported_at": ts(5),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 201, f"Setup failed: {r.text}"
    return r.json()["report_id"]


# ── POST /api/v1/reports ──────────────────────────────────────────────────────

def test_create_report_returns_201_with_report_id():
    payload = {
        "container_id": "container-py-001",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 201
    assert "report_id" in r.json()


def test_create_report_future_timestamp_returns_400():
    payload = {
        "container_id": "container-py-002",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 256,
        "reported_at": (datetime.now(timezone.utc) + timedelta(days=1)).strftime('%Y-%m-%dT%H:%M:%SZ'),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_cpu_over_100_returns_400():
    payload = {
        "container_id": "container-py-003",
        "app_name": "billing-service",
        "cpu_usage_percent": 150.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_negative_memory_returns_400():
    payload = {
        "container_id": "container-py-004",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": -1,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_blank_container_id_returns_400():
    payload = {
        "container_id": "",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_container_id_too_short_returns_400():
    payload = {
        "container_id": "ab",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_container_id_invalid_chars_returns_400():
    payload = {
        "container_id": "web@1",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_cpu_zero_returns_201():
    payload = {
        "container_id": "container-py-005",
        "app_name": "billing-service",
        "cpu_usage_percent": 0.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 201


def test_create_report_cpu_exactly_100_returns_201():
    payload = {
        "container_id": "container-py-006",
        "app_name": "billing-service",
        "cpu_usage_percent": 100.0,
        "memory_usage_mb": 256,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 201


def test_create_report_memory_zero_returns_400():
    payload = {
        "container_id": "container-py-007",
        "app_name": "billing-service",
        "cpu_usage_percent": 50.0,
        "memory_usage_mb": 0,
        "reported_at": ts(1),
    }
    r = requests.post(API, json=payload)
    assert r.status_code == 400


def test_create_report_response_report_id_is_uuid(created_report_id):
    import re
    uuid_pattern = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
    assert uuid_pattern.match(created_report_id), f"report_id is not a valid UUID: {created_report_id}"


# ── GET /api/v1/reports/{id} ──────────────────────────────────────────────────

def test_get_report_returns_200_with_correct_fields(created_report_id):
    r = requests.get(f"{API}/{created_report_id}")
    assert r.status_code == 200
    body = r.json()
    assert body["container_id"] == "container-py-test"
    assert body["app_name"] == "billing-service"
    assert "cpu_usage_percent" in body
    assert "memory_usage_mb" in body


def test_get_report_not_found_returns_404():
    r = requests.get(f"{API}/00000000-0000-0000-0000-000000000000")
    assert r.status_code == 404


def test_get_report_returns_all_expected_fields(created_report_id):
    r = requests.get(f"{API}/{created_report_id}")
    assert r.status_code == 200
    body = r.json()
    assert body["report_id"] == created_report_id
    assert body["cpu_usage_percent"] == 72.5
    assert body["memory_usage_mb"] == 512
    assert "reported_at" in body


# ── GET /api/v1/reports/summary ───────────────────────────────────────────────

def test_summary_returns_200_with_aggregation_fields(created_report_id):
    params = {
        "container_id": "container-py-test",
        "app_name": "billing-service",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text}"
    body = r.json()
    assert "total_reports" in body
    assert "avg_cpu_percent" in body
    assert "peak_cpu_percent" in body
    assert "avg_memory_mb" in body
    assert "peak_memory_mb" in body


def test_summary_counts_created_report(created_report_id):
    params = {
        "container_id": "container-py-test",
        "app_name": "billing-service",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 200
    body = r.json()
    assert body["total_reports"] >= 1
    assert body["avg_cpu_percent"] > 0
    assert body["peak_cpu_percent"] >= body["avg_cpu_percent"]


def test_summary_no_data_for_unknown_container():
    params = {
        "container_id": "no-such-container-xyz",
        "app_name": "no-such-app-xyz",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 200
    body = r.json()
    assert body["total_reports"] == 0


def test_summary_start_after_end_returns_400():
    params = {
        "container_id": "container-py-test",
        "app_name": "billing-service",
        "start": ts(0),
        "end": ts(60),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 400


def test_summary_missing_app_name_returns_400():
    params = {
        "container_id": "container-py-test",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 400


def test_summary_missing_container_id_returns_400():
    params = {
        "app_name": "billing-service",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 400


# ── GET /api/v1/reports/top-consumers ────────────────────────────────────────

def test_top_consumers_returns_200_with_results():
    r = requests.get(f"{API}/top-consumers", params={"limit": 5})
    assert r.status_code == 200
    assert "results" in r.json()


def test_top_consumers_limit_over_100_returns_400():
    r = requests.get(f"{API}/top-consumers", params={"limit": 101})
    assert r.status_code == 400


def test_top_consumers_limit_zero_returns_400():
    r = requests.get(f"{API}/top-consumers", params={"limit": 0})
    assert r.status_code == 400


def test_top_consumers_limit_1_returns_at_most_1_result():
    r = requests.get(f"{API}/top-consumers", params={"limit": 1})
    assert r.status_code == 200
    assert len(r.json()["results"]) <= 1


def test_top_consumers_results_sorted_by_avg_cpu_desc():
    r = requests.get(f"{API}/top-consumers", params={"limit": 10})
    assert r.status_code == 200
    results = r.json()["results"]
    if len(results) >= 2:
        for i in range(len(results) - 1):
            assert results[i]["avg_cpu_percent"] >= results[i + 1]["avg_cpu_percent"]

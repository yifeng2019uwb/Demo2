import os
import pytest
import requests
from datetime import datetime, timedelta

BASE_URL = os.getenv("BASE_URL", "https://report-app-88qqj.ondigitalocean.app")
API = f"{BASE_URL}/api/v1/reports"


def ts(delta_minutes=0):
    return (datetime.now() - timedelta(minutes=delta_minutes)).replace(microsecond=0).isoformat()


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
    assert r.status_code == 201, f"Setup failed: {r.body}"
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
        "reported_at": (datetime.now() + timedelta(days=1)).isoformat(),
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


# ── GET /api/v1/reports/summary ───────────────────────────────────────────────

def test_summary_returns_200_with_aggregation_fields(created_report_id):
    params = {
        "container_id": "container-py-test",
        "app_name": "billing-service",
        "start": ts(60),
        "end": ts(0),
    }
    r = requests.get(f"{API}/summary", params=params)
    assert r.status_code == 200
    body = r.json()
    assert "total_reports" in body
    assert "avg_cpu_percent" in body
    assert "peak_cpu_percent" in body
    assert "avg_memory_mb" in body
    assert "peak_memory_mb" in body


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

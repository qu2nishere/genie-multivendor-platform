from fastapi.testclient import TestClient

from app import health
from app.main import app


def test_health_reports_loading_before_startup(client):
    health.state.started = False
    response = client.get("/health")
    assert response.status_code == 503
    assert response.json()["status"] == "LOADING"


def test_health_up_after_lifespan_with_model_flag():
    with TestClient(app) as c:
        body = c.get("/health").json()
        assert body["status"] == "UP"
        assert isinstance(body["models_loaded"], bool)
        assert body["pipeline_version"]

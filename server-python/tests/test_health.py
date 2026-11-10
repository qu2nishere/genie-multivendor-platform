def test_health_reports_loading_before_models(client):
    response = client.get("/health")
    assert response.status_code == 503
    assert response.json() == {"status": "LOADING"}

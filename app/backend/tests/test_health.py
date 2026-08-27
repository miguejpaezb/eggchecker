from app.main import create_app
from fastapi.testclient import TestClient


def test_health_retorna_estado_ok() -> None:
    """Verifica que el endpoint GET /api/health responde con estado ok.

    Es la primera prueba del proyecto: confirma que la aplicación arranca
    y que el contrato de sondeo del backend es estable.
    """
    client = TestClient(create_app())

    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}

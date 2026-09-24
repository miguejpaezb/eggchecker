from datetime import date
from pathlib import Path

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
EDAD_INICIAL = 16
EDAD_DECISION = 72


@pytest.fixture()
def cliente(tmp_path: Path):
    """Levanta la API con una base SQLite temporal cargada con el seed.

    Returns:
        TestClient: Cliente de prueba listo para usar.
    """
    db_path = tmp_path / "test.db"
    cargar_seed(db_path)
    engine = create_engine(
        f"sqlite:///{db_path}", connect_args={"check_same_thread": False}
    )
    session_prueba = sessionmaker(bind=engine, autocommit=False, autoflush=False)

    def override_get_db():
        db = session_prueba()
        try:
            yield db
        finally:
            db.close()

    app = create_app()
    app.dependency_overrides[get_db] = override_get_db
    client = TestClient(app)
    yield client
    client.close()
    engine.dispose()


def _registrar_usuario(cliente, correo: str) -> dict:
    """Registra un usuario y devuelve su header de autorización."""
    respuesta = cliente.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": correo,
            "contrasena": CLAVE_VALIDA,
        },
    )
    assert respuesta.status_code == 201
    id_usuario = respuesta.json()["id_usuario"]
    return {"Authorization": f"Bearer {crear_token_acceso(sub=str(id_usuario))}"}


def _camada_en_limite(cliente, headers, nombre: str = "Camada Prueba") -> dict:
    """Crea una camada y la lleva a la semana de decisión."""
    creacion = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": nombre,
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    assert creacion.status_code == 201
    id_camada = creacion.json()["id_camada"]
    for _ in range(EDAD_DECISION - EDAD_INICIAL):
        respuesta = cliente.post(
            f"/api/camadas/{id_camada}/avanzar-semana", headers=headers
        )
        assert respuesta.status_code == 200
    return respuesta.json()


def test_camada_en_limite_genera_notificacion(cliente) -> None:
    """Una camada que llega a 72 semanas genera su aviso al listar."""
    headers = _registrar_usuario(cliente, "notif1@test.com")
    camada = _camada_en_limite(cliente, headers, "Camada La Esperanza")

    respuesta = cliente.get("/api/notificaciones", headers=headers)

    assert respuesta.status_code == 200
    notificaciones = respuesta.json()
    assert len(notificaciones) == 1
    aviso = notificaciones[0]
    assert aviso["id_camada"] == camada["id_camada"]
    assert aviso["titulo"] == "Camada al límite"
    assert "Camada La Esperanza" in aviso["mensaje"]
    assert aviso["leida"] is False


def test_no_genera_antes_de_72_semanas(cliente) -> None:
    """Una camada por debajo de 72 semanas no genera aviso."""
    headers = _registrar_usuario(cliente, "notif2@test.com")
    creacion = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": "Camada Joven",
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": 50,
        },
        headers=headers,
    )
    id_camada = creacion.json()["id_camada"]
    for _ in range(EDAD_DECISION - EDAD_INICIAL - 1):
        cliente.post(f"/api/camadas/{id_camada}/avanzar-semana", headers=headers)

    respuesta = cliente.get("/api/notificaciones", headers=headers)

    assert respuesta.json() == []


def test_no_duplica_la_notificacion_al_listar(cliente) -> None:
    """Listar varias veces no crea avisos repetidos por camada."""
    headers = _registrar_usuario(cliente, "notif3@test.com")
    _camada_en_limite(cliente, headers)

    cliente.get("/api/notificaciones", headers=headers)
    respuesta = cliente.get("/api/notificaciones", headers=headers)

    assert len(respuesta.json()) == 1


def test_marcar_notificacion_como_leida(cliente) -> None:
    """Marcar como leída conserva el aviso pero cambia su estado."""
    headers = _registrar_usuario(cliente, "notif4@test.com")
    _camada_en_limite(cliente, headers)
    aviso = cliente.get("/api/notificaciones", headers=headers).json()[0]

    respuesta = cliente.post(
        f"/api/notificaciones/{aviso['id_notificacion']}/leer", headers=headers
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["leida"] is True
    assert len(cliente.get("/api/notificaciones", headers=headers).json()) == 1


def test_marcar_todas_como_leidas(cliente) -> None:
    """El endpoint de marcar todas actualiza todos los avisos."""
    headers = _registrar_usuario(cliente, "notif5@test.com")
    _camada_en_limite(cliente, headers, "Camada Uno")
    _camada_en_limite(cliente, headers, "Camada Dos")

    respuesta = cliente.post("/api/notificaciones/leer-todas", headers=headers)

    assert respuesta.status_code == 204
    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    assert len(avisos) == 2
    assert all(aviso["leida"] for aviso in avisos)


def test_eliminar_notificacion_no_la_regenera(cliente) -> None:
    """Eliminar un aviso con la X no lo vuelve a crear al listar."""
    headers = _registrar_usuario(cliente, "notif6@test.com")
    _camada_en_limite(cliente, headers)
    aviso = cliente.get("/api/notificaciones", headers=headers).json()[0]

    eliminar = cliente.delete(
        f"/api/notificaciones/{aviso['id_notificacion']}", headers=headers
    )
    assert eliminar.status_code == 204

    assert cliente.get("/api/notificaciones", headers=headers).json() == []


def test_notificacion_de_otro_usuario_devuelve_404(cliente) -> None:
    """Un usuario no puede operar sobre avisos ajenos."""
    dueno = _registrar_usuario(cliente, "notif7@test.com")
    _camada_en_limite(cliente, dueno)
    aviso = cliente.get("/api/notificaciones", headers=dueno).json()[0]
    intruso = _registrar_usuario(cliente, "notif8@test.com")

    leer = cliente.post(
        f"/api/notificaciones/{aviso['id_notificacion']}/leer", headers=intruso
    )
    eliminar = cliente.delete(
        f"/api/notificaciones/{aviso['id_notificacion']}", headers=intruso
    )

    assert leer.status_code == 404
    assert eliminar.status_code == 404


def test_listar_notificaciones_sin_token_devuelve_401(cliente) -> None:
    """Listar notificaciones sin token responde 401."""
    respuesta = cliente.get("/api/notificaciones")

    assert respuesta.status_code == 401

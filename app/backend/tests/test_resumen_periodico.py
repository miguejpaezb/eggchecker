"""Pruebas del recordatorio periódico para revisar los reportes."""

from datetime import date, timedelta
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
HOY = date.today()


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
    token = crear_token_acceso(sub=str(respuesta.json()["id_usuario"]))
    return {"Authorization": f"Bearer {token}"}


def _configurar(
    cliente,
    headers,
    activo: bool = True,
    frecuencia: str = "semanal",
    dia: int | None = None,
) -> None:
    """Guarda la preferencia del recordatorio y apaga las demás alertas."""
    respuesta = cliente.put(
        "/api/usuarios/me/notificaciones",
        headers=headers,
        json={
            "notif_produccion_baja": False,
            "notif_stock_bajo": False,
            "notif_vacunacion": False,
            "notif_resumen_semanal": activo,
            "notif_resumen_frecuencia": frecuencia,
            "notif_resumen_dia": HOY.weekday() if dia is None else dia,
        },
    )
    assert respuesta.status_code == 200, respuesta.text


def _avisos_resumen(cliente, headers) -> list:
    """Devuelve solo los recordatorios de reportes del usuario."""
    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    return [aviso for aviso in avisos if aviso["tipo"] == "resumen_periodico"]


def test_semanal_avisa_el_dia_elegido(cliente) -> None:
    """El recordatorio semanal llega el día configurado con 7 días de rango."""
    headers = _registrar_usuario(cliente, "res1@test.com")
    _configurar(cliente, headers)

    avisos = _avisos_resumen(cliente, headers)

    assert len(avisos) == 1
    aviso = avisos[0]
    assert aviso["titulo"] == "Recordatorio de reportes"
    assert aviso["fecha_referencia"] == HOY.isoformat()
    desde = HOY - timedelta(days=6)
    assert f"{desde:%d/%m}" in aviso["mensaje"]
    assert f"{HOY:%d/%m}" in aviso["mensaje"]


def test_semanal_usa_la_ultima_ocurrencia(cliente) -> None:
    """Si hoy no es el día elegido, se avisa la última vez que tocó."""
    headers = _registrar_usuario(cliente, "res2@test.com")
    dia = (HOY.weekday() + 2) % 7
    _configurar(cliente, headers, dia=dia)

    avisos = _avisos_resumen(cliente, headers)

    assert len(avisos) == 1
    fecha = date.fromisoformat(avisos[0]["fecha_referencia"])
    assert fecha.weekday() == dia
    assert fecha <= HOY
    assert (HOY - fecha).days < 7


def test_preferencia_apagada_no_avisa(cliente) -> None:
    """Con el recordatorio apagado no se genera nada."""
    headers = _registrar_usuario(cliente, "res3@test.com")
    _configurar(cliente, headers, activo=False)

    assert _avisos_resumen(cliente, headers) == []


def test_no_duplica_el_aviso(cliente) -> None:
    """Listar varias veces no repite el recordatorio de la misma fecha."""
    headers = _registrar_usuario(cliente, "res4@test.com")
    _configurar(cliente, headers)

    cliente.get("/api/notificaciones", headers=headers)
    avisos = _avisos_resumen(cliente, headers)

    assert len(avisos) == 1


def test_aviso_eliminado_no_se_regenera(cliente) -> None:
    """Si el usuario descarta el recordatorio, no vuelve a aparecer."""
    headers = _registrar_usuario(cliente, "res5@test.com")
    _configurar(cliente, headers)
    aviso = _avisos_resumen(cliente, headers)[0]

    borrar = cliente.delete(
        f"/api/notificaciones/{aviso['id_notificacion']}", headers=headers
    )

    assert borrar.status_code == 204
    assert _avisos_resumen(cliente, headers) == []


def test_quincenal_cubre_14_dias(cliente) -> None:
    """El recordatorio quincenal abarca dos semanas y cae en el día elegido."""
    headers = _registrar_usuario(cliente, "res6@test.com")
    dia = HOY.weekday()
    _configurar(cliente, headers, frecuencia="quincenal", dia=dia)

    avisos = _avisos_resumen(cliente, headers)

    assert len(avisos) == 1
    aviso = avisos[0]
    fecha = date.fromisoformat(aviso["fecha_referencia"])
    assert fecha.weekday() == dia
    assert 0 <= (HOY - fecha).days < 14
    desde = fecha - timedelta(days=13)
    assert f"{desde:%d/%m}" in aviso["mensaje"]


def test_mensual_cae_en_el_primer_dia_del_mes(cliente) -> None:
    """El recordatorio mensual usa el primer día elegido del mes."""
    headers = _registrar_usuario(cliente, "res7@test.com")
    dia = HOY.weekday()
    _configurar(cliente, headers, frecuencia="mensual", dia=dia)

    avisos = _avisos_resumen(cliente, headers)

    assert len(avisos) == 1
    aviso = avisos[0]
    fecha = date.fromisoformat(aviso["fecha_referencia"])
    assert fecha.weekday() == dia
    assert fecha.day <= 7
    assert fecha <= HOY
    desde = fecha - timedelta(days=29)
    assert f"{desde:%d/%m}" in aviso["mensaje"]

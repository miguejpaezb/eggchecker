"""Pruebas del filtrado de preferencias en el backend.

Los avisos ya no se generan si el avicultor apagó esa alerta; los que ya
existían se conservan como historial.
"""

from datetime import date, timedelta
from decimal import Decimal
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
    token = crear_token_acceso(sub=str(respuesta.json()["id_usuario"]))
    return {"Authorization": f"Bearer {token}"}


def _preferencias(
    cliente,
    headers,
    stock: bool = False,
    camada_limite: bool = False,
) -> None:
    """Guarda las preferencias apagando el resto de alertas."""
    respuesta = cliente.put(
        "/api/usuarios/me/notificaciones",
        headers=headers,
        json={
            "notif_produccion_baja": False,
            "notif_stock_bajo": stock,
            "notif_vacunacion": False,
            "notif_resumen_semanal": False,
            "notif_camada_limite": camada_limite,
        },
    )
    assert respuesta.status_code == 200, respuesta.text


def _categoria_id(cliente, headers, nombre: str = "medicamento") -> int:
    """Busca el identificador de una categoría del catálogo."""
    categorias = cliente.get("/api/categorias-insumo", headers=headers).json()
    return next(
        categoria["id_categoria"]
        for categoria in categorias
        if categoria["nombre_categ"] == nombre
    )


def _crear_insumo(cliente, headers, stock: int = 5, umbral: int = 10) -> dict:
    """Crea un insumo con el stock y el umbral indicados."""
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": _categoria_id(cliente, headers),
            "nombre_insumo": "Newcastle",
            "unidad_medida": "ml",
            "stock_actual": stock,
            "umbral_minimo": umbral,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def _avisos_stock(cliente, headers) -> list:
    """Devuelve solo los avisos de stock del usuario."""
    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    return [aviso for aviso in avisos if aviso["id_insumo"] is not None]


def _camada_en_limite(cliente, headers, nombre: str = "Camada Prueba") -> dict:
    """Crea una camada y la lleva a la semana de decisión."""
    creacion = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": nombre,
            "fecha_ingreso": (date.today() - timedelta(days=1)).isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    assert creacion.status_code == 201
    id_camada = creacion.json()["id_camada"]
    respuesta = creacion
    for _ in range(EDAD_DECISION - EDAD_INICIAL):
        respuesta = cliente.post(
            f"/api/camadas/{id_camada}/avanzar-semana", headers=headers
        )
        assert respuesta.status_code == 200
    return respuesta.json()


def test_stock_apagado_no_genera_aviso(cliente) -> None:
    """Con la alerta de stock apagada no se crea el aviso bajo umbral."""
    headers = _registrar_usuario(cliente, "pref1@test.com")
    _preferencias(cliente, headers, stock=False)

    _crear_insumo(cliente, headers, stock=5, umbral=10)

    assert _avisos_stock(cliente, headers) == []


def test_stock_encendido_genera_aviso(cliente) -> None:
    """Con la alerta encendida el insumo bajo umbral sí avisa."""
    headers = _registrar_usuario(cliente, "pref2@test.com")
    _preferencias(cliente, headers, stock=True)

    _crear_insumo(cliente, headers, stock=5, umbral=10)

    avisos = _avisos_stock(cliente, headers)
    assert len(avisos) == 1
    assert avisos[0]["tipo"] == "stock_bajo"


def test_movimiento_con_stock_apagado_no_avisa(cliente) -> None:
    """Una salida que deja el insumo en cero no avisa si está apagado."""
    headers = _registrar_usuario(cliente, "pref3@test.com")
    _preferencias(cliente, headers, stock=False)
    insumo = _crear_insumo(cliente, headers, stock=20, umbral=1)

    salida = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "salida", "cantidad": 20},
        headers=headers,
    )

    assert salida.status_code == 201
    assert _avisos_stock(cliente, headers) == []


def test_camada_limite_apagada_no_genera_aviso(cliente) -> None:
    """Con la alerta de camada al límite apagada no se avisa a las 72 semanas."""
    headers = _registrar_usuario(cliente, "pref4@test.com")
    _preferencias(cliente, headers, camada_limite=False)

    _camada_en_limite(cliente, headers)

    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    assert [a for a in avisos if a["tipo"] == "camada_limite"] == []


def test_camada_limite_encendida_genera_aviso(cliente) -> None:
    """Con la alerta encendida la camada a las 72 semanas avisa."""
    headers = _registrar_usuario(cliente, "pref5@test.com")
    _preferencias(cliente, headers, camada_limite=True)

    _camada_en_limite(cliente, headers)

    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    assert len([a for a in avisos if a["tipo"] == "camada_limite"]) == 1


def test_aviso_existente_se_conserva_al_apagar(cliente) -> None:
    """Apagar la alerta no borra los avisos que ya estaban en la lista."""
    headers = _registrar_usuario(cliente, "pref6@test.com")
    _preferencias(cliente, headers, stock=True)
    _crear_insumo(cliente, headers, stock=5, umbral=10)
    assert len(_avisos_stock(cliente, headers)) == 1

    _preferencias(cliente, headers, stock=False)

    assert len(_avisos_stock(cliente, headers)) == 1

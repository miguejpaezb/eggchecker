"""Pruebas del CRUD de recordatorios sanitarios y sus ocurrencias."""

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
HOY = date.today()


@pytest.fixture()
def cliente(tmp_path: Path):
    """Levanta la API con una base SQLite temporal cargada con el seed.

    Returns:
        tuple: Cliente de prueba y ruta de la base temporal.
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
    yield client, db_path
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


def _categoria_id(cliente, headers, nombre: str) -> int:
    """Busca en el catálogo el identificador de una categoría."""
    categorias = cliente.get("/api/categorias-insumo", headers=headers).json()
    return next(
        categoria["id_categoria"]
        for categoria in categorias
        if categoria["nombre_categ"] == nombre
    )


def _crear_insumo(
    cliente,
    headers,
    categoria: str = "medicamento",
    nombre: str = "Newcastle",
    unidad: str = "ml",
    stock: int = 20,
) -> dict:
    """Crea un insumo de la categoría indicada."""
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": _categoria_id(cliente, headers, categoria),
            "nombre_insumo": nombre,
            "unidad_medida": unidad,
            "stock_actual": stock,
            "umbral_minimo": 1,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_camada(cliente, headers, nombre: str = "Camada Uno") -> dict:
    """Crea una camada activa de 100 aves."""
    respuesta = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": nombre,
            "fecha_ingreso": (HOY - timedelta(days=1)).isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_recordatorio(cliente, headers, **cambios) -> dict:
    """Crea un recordatorio con valores por defecto razonables."""
    datos = {
        "id_insumo": _crear_insumo(cliente, headers)["id_insumo"],
        "modo": "cada_dias",
        "intervalo_dias": 15,
        "fecha_inicio": HOY.isoformat(),
        "cantidad": 5,
    }
    datos.update(cambios)
    respuesta = cliente.post("/api/recordatorios", json=datos, headers=headers)
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def _dosis(cliente, headers, id_recordatorio: int, **params) -> list:
    """Consulta las dosis programadas de un recordatorio."""
    respuesta = cliente.get(
        f"/api/recordatorios/{id_recordatorio}/dosis",
        params=params,
        headers=headers,
    )
    assert respuesta.status_code == 200, respuesta.text
    return respuesta.json()


def test_crear_cada_dias_genera_ocurrencias(cliente) -> None:
    """Un plan cada 15 días materializa las dosis del horizonte."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec1@test.com")
    camada = _crear_camada(client, headers)

    regla = _crear_recordatorio(
        client, headers, id_camada=camada["id_camada"], intervalo_dias=15
    )

    assert regla["modo"] == "cada_dias"
    assert regla["proxima_dosis"] == HOY.isoformat()
    assert regla["dosis_pendientes"] == 7
    assert regla["nombre_insumo"] == "Newcastle"
    assert regla["unidad_medida"] == "ml"
    assert Decimal(regla["stock_actual"]) == Decimal("20.00")

    dosis = _dosis(client, headers, regla["id_recordatorio"])
    fechas = [date.fromisoformat(item["fecha_programada"]) for item in dosis]
    assert len(fechas) == 7
    assert [item["numero_dosis"] for item in dosis] == list(range(1, 8))
    assert all(
        (segunda - primera).days == 15
        for primera, segunda in zip(fechas, fechas[1:])
    )


def test_crear_dias_semana_solo_esos_dias(cliente) -> None:
    """Un plan de lunes y jueves solo genera esos días de la semana."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec2@test.com")
    camada = _crear_camada(client, headers)

    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        modo="dias_semana",
        intervalo_dias=None,
        dias_semana=[0, 3],
    )

    dosis = _dosis(client, headers, regla["id_recordatorio"])
    assert regla["dias_semana"] == [0, 3]
    assert dosis
    assert {date.fromisoformat(item["fecha_programada"]).weekday() for item in dosis} == {
        0,
        3,
    }


def test_crear_fechas_exactas(cliente) -> None:
    """Un plan con fechas exactas genera una dosis por cada fecha."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec3@test.com")
    camada = _crear_camada(client, headers)
    fechas = [HOY + timedelta(days=2), HOY + timedelta(days=16)]

    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        modo="fechas",
        intervalo_dias=None,
        fecha_inicio=None,
        fechas=[fecha.isoformat() for fecha in fechas],
    )

    assert regla["fecha_inicio"] == fechas[0].isoformat()
    dosis = _dosis(client, headers, regla["id_recordatorio"])
    assert [item["fecha_programada"] for item in dosis] == [
        fecha.isoformat() for fecha in fechas
    ]


def test_global_genera_una_dosis_por_camada(cliente) -> None:
    """Un recordatorio global se materializa en cada camada activa."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec4@test.com")
    _crear_camada(client, headers, "Camada Norte")
    _crear_camada(client, headers, "Camada Sur")

    regla = _crear_recordatorio(client, headers, intervalo_dias=30)

    assert regla["id_camada"] is None
    assert regla["nombre_camada"] is None
    dosis = _dosis(client, headers, regla["id_recordatorio"])
    camadas = {item["nombre_camada"] for item in dosis}
    assert camadas == {"Camada Norte", "Camada Sur"}
    # Cada camada recibe la misma secuencia de dosis.
    primeras = [item for item in dosis if item["numero_dosis"] == 1]
    assert len(primeras) == 2


def test_insumo_de_otra_categoria_devuelve_400(cliente) -> None:
    """Un insumo de alimento no sirve para un recordatorio sanitario."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec5@test.com")
    insumo = _crear_insumo(client, headers, categoria="alimento", nombre="Maíz")

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": insumo["id_insumo"],
            "modo": "cada_dias",
            "intervalo_dias": 15,
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 1,
        },
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert "medicamento" in respuesta.json()["detail"]


def test_insumo_de_otro_usuario_devuelve_404(cliente) -> None:
    """No se puede usar el inventario de otro avicultor."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "rec6@test.com")
    intruso = _registrar_usuario(client, "rec7@test.com")
    insumo = _crear_insumo(client, dueno)

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": insumo["id_insumo"],
            "modo": "cada_dias",
            "intervalo_dias": 15,
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 1,
        },
        headers=intruso,
    )

    assert respuesta.status_code == 404


def test_camada_de_otro_usuario_devuelve_404(cliente) -> None:
    """No se puede programar sobre una camada ajena."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "rec8@test.com")
    intruso = _registrar_usuario(client, "rec9@test.com")
    camada = _crear_camada(client, dueno)

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": _crear_insumo(client, intruso)["id_insumo"],
            "id_camada": camada["id_camada"],
            "modo": "cada_dias",
            "intervalo_dias": 15,
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 1,
        },
        headers=intruso,
    )

    assert respuesta.status_code == 404


def test_modo_sin_sus_campos_devuelve_422(cliente) -> None:
    """El modo cada_dias exige el intervalo entre dosis."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec10@test.com")

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": _crear_insumo(client, headers)["id_insumo"],
            "modo": "cada_dias",
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 1,
        },
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_fechas_pasadas_devuelve_422(cliente) -> None:
    """No se aceptan fechas de aplicación ya pasadas."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec11@test.com")

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": _crear_insumo(client, headers)["id_insumo"],
            "modo": "fechas",
            "fechas": [(HOY - timedelta(days=3)).isoformat()],
            "cantidad": 1,
        },
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_cantidad_cero_devuelve_422(cliente) -> None:
    """La dosis debe ser mayor que cero."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec12@test.com")

    respuesta = client.post(
        "/api/recordatorios",
        json={
            "id_insumo": _crear_insumo(client, headers)["id_insumo"],
            "modo": "cada_dias",
            "intervalo_dias": 7,
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 0,
        },
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_actualizar_reemplaza_las_dosis_futuras(cliente) -> None:
    """Al cambiar la frecuencia se recalculan las dosis pendientes."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec13@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(
        client, headers, id_camada=camada["id_camada"], intervalo_dias=15
    )

    respuesta = client.put(
        f"/api/recordatorios/{regla['id_recordatorio']}",
        json={
            "id_insumo": regla["id_insumo"],
            "id_camada": camada["id_camada"],
            "modo": "cada_dias",
            "intervalo_dias": 30,
            "fecha_inicio": HOY.isoformat(),
            "cantidad": 8,
        },
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["intervalo_dias"] == 30
    assert Decimal(respuesta.json()["cantidad"]) == Decimal("8")
    dosis = _dosis(client, headers, regla["id_recordatorio"])
    assert len(dosis) == 4
    assert [item["numero_dosis"] for item in dosis] == [1, 2, 3, 4]


def test_eliminar_desactiva_y_borra_pendientes(cliente) -> None:
    """Eliminar desactiva la regla y descarta las dosis que faltaban."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec14@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(client, headers, id_camada=camada["id_camada"])
    assert _dosis(client, headers, regla["id_recordatorio"])

    borrar = client.delete(
        f"/api/recordatorios/{regla['id_recordatorio']}", headers=headers
    )

    assert borrar.status_code == 204
    assert client.get("/api/recordatorios", headers=headers).json() == []
    assert _dosis(client, headers, regla["id_recordatorio"]) == []


def test_listar_filtra_por_camada_e_incluye_globales(cliente) -> None:
    """El listado de una camada suma los recordatorios de la granja."""
    client, _ = cliente
    headers = _registrar_usuario(client, "rec15@test.com")
    camada = _crear_camada(client, headers, "Camada Norte")
    propio = _crear_recordatorio(client, headers, id_camada=camada["id_camada"])
    global_ = _crear_recordatorio(client, headers, intervalo_dias=30)

    con_globales = client.get(
        "/api/recordatorios",
        params={"id_camada": camada["id_camada"]},
        headers=headers,
    ).json()
    sin_globales = client.get(
        "/api/recordatorios",
        params={"id_camada": camada["id_camada"], "incluir_globales": False},
        headers=headers,
    ).json()

    assert {item["id_recordatorio"] for item in con_globales} == {
        propio["id_recordatorio"],
        global_["id_recordatorio"],
    }
    assert [item["id_recordatorio"] for item in sin_globales] == [
        propio["id_recordatorio"]
    ]


def test_recordatorio_de_otro_usuario_devuelve_404(cliente) -> None:
    """Un avicultor no puede ver ni modificar reglas ajenas."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "rec16@test.com")
    intruso = _registrar_usuario(client, "rec17@test.com")
    regla = _crear_recordatorio(client, dueno)

    ver = client.get(
        f"/api/recordatorios/{regla['id_recordatorio']}", headers=intruso
    )
    borrar = client.delete(
        f"/api/recordatorios/{regla['id_recordatorio']}", headers=intruso
    )
    dosis = client.get(
        f"/api/recordatorios/{regla['id_recordatorio']}/dosis", headers=intruso
    )

    assert ver.status_code == 404
    assert borrar.status_code == 404
    assert dosis.status_code == 404

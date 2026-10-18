"""Pruebas de los avisos de recordatorios y del registro de aplicaciones.

Cubre el aviso del día anterior y del mismo día, el resumen de un
recordatorio global y la aplicación de la dosis en una sola transacción:
descuenta inventario, crea el evento sanitario y marca la dosis.
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
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
HOY = date.today()
MANANA = HOY + timedelta(days=1)


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


def _activar_recordatorios(cliente, headers) -> None:
    """Enciende la preferencia de recordatorios sanitarios."""
    respuesta = cliente.put(
        "/api/usuarios/me/notificaciones",
        headers=headers,
        json={
            "notif_produccion_baja": False,
            "notif_stock_bajo": False,
            "notif_vacunacion": True,
            "notif_resumen_semanal": False,
        },
    )
    assert respuesta.status_code == 200


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
    stock: int = 20,
) -> dict:
    """Crea un insumo de la categoría indicada."""
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": _categoria_id(cliente, headers, categoria),
            "nombre_insumo": nombre,
            "unidad_medida": "ml",
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
    """Crea un recordatorio de hoy y mañana para probar los dos avisos."""
    datos = {
        "id_insumo": _crear_insumo(cliente, headers)["id_insumo"],
        "modo": "cada_dias",
        "intervalo_dias": 1,
        "fecha_inicio": HOY.isoformat(),
        "fecha_fin": MANANA.isoformat(),
        "cantidad": 5,
    }
    datos.update(cambios)
    respuesta = cliente.post("/api/recordatorios", json=datos, headers=headers)
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def _dosis(cliente, headers, id_recordatorio: int) -> list:
    """Lista todas las dosis de un recordatorio."""
    respuesta = cliente.get(
        f"/api/recordatorios/{id_recordatorio}/dosis", headers=headers
    )
    assert respuesta.status_code == 200
    return respuesta.json()


def _avisos_recordatorio(cliente, headers) -> list:
    """Devuelve solo los avisos de recordatorios sanitarios."""
    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    return [aviso for aviso in avisos if aviso["tipo"] == "recordatorio_sanitario"]


def _eventos(db_path: Path, id_camada: int) -> list:
    """Lee los eventos sanitarios de una camada directamente de la base."""
    engine = create_engine(f"sqlite:///{db_path}")
    with engine.connect() as conn:
        filas = conn.execute(
            text(
                "SELECT tipo_evento, descripcion FROM evento_sanitario "
                "WHERE id_camada = :camada"
            ),
            {"camada": id_camada},
        ).fetchall()
    engine.dispose()
    return filas


def test_avisa_el_dia_antes_y_el_mismo_dia(cliente) -> None:
    """Un plan de hoy y mañana genera dos avisos con su momento."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl1@test.com")
    _activar_recordatorios(client, headers)
    camada = _crear_camada(client, headers)
    _crear_recordatorio(client, headers, id_camada=camada["id_camada"])

    avisos = _avisos_recordatorio(client, headers)

    assert len(avisos) == 2
    por_momento = {aviso["momento"]: aviso for aviso in avisos}
    assert set(por_momento) == {"previo", "hoy"}
    assert "Mañana" in por_momento["previo"]["mensaje"]
    assert "Hoy" in por_momento["hoy"]["mensaje"]
    assert por_momento["hoy"]["fecha_referencia"] == HOY.isoformat()
    assert por_momento["previo"]["fecha_referencia"] == MANANA.isoformat()
    assert "Newcastle" in por_momento["hoy"]["titulo"]
    assert "(dosis 1 de 2)" in por_momento["hoy"]["mensaje"]
    assert "(dosis 2 de 2)" in por_momento["previo"]["mensaje"]
    assert por_momento["hoy"]["id_recordatorio"] == por_momento["previo"][
        "id_recordatorio"
    ]


def test_preferencia_apagada_no_avisa(cliente) -> None:
    """Con `notif_vacunacion` en falso no se generan avisos."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl2@test.com")
    camada = _crear_camada(client, headers)
    _crear_recordatorio(client, headers, id_camada=camada["id_camada"])

    assert _avisos_recordatorio(client, headers) == []


def test_global_resume_las_camadas_en_un_solo_aviso(cliente) -> None:
    """Un recordatorio global avisa una vez y resume las camadas."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl3@test.com")
    _activar_recordatorios(client, headers)
    _crear_camada(client, headers, "Camada Norte")
    _crear_camada(client, headers, "Camada Sur")
    _crear_recordatorio(client, headers)

    avisos = _avisos_recordatorio(client, headers)

    assert len(avisos) == 2
    hoy = next(aviso for aviso in avisos if aviso["momento"] == "hoy")
    assert "Camada Norte" in hoy["mensaje"]
    assert "Camada Sur" in hoy["mensaje"]
    assert hoy["id_camada"] is None


def test_no_duplica_el_aviso_del_dia(cliente) -> None:
    """Listar varias veces no repite el aviso de la misma dosis."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl4@test.com")
    _activar_recordatorios(client, headers)
    camada = _crear_camada(client, headers)
    _crear_recordatorio(client, headers, id_camada=camada["id_camada"])

    client.get("/api/notificaciones", headers=headers)
    avisos = _avisos_recordatorio(client, headers)

    assert len(avisos) == 2


def test_aplicar_descuenta_stock_y_crea_evento(cliente) -> None:
    """Aplicar una dosis descuenta el insumo y deja el evento sanitario."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "apl5@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]

    respuesta = client.post(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar",
        json={},
        headers=headers,
    )

    assert respuesta.status_code == 200, respuesta.text
    cuerpo = respuesta.json()
    assert Decimal(cuerpo["stock_resultante"]) == Decimal("15")
    assert cuerpo["id_movimiento"] and cuerpo["id_evento"]
    assert cuerpo["nombre_insumo"] == "Newcastle"

    insumo = client.get(
        f"/api/insumos/{regla['id_insumo']}", headers=headers
    ).json()
    assert Decimal(insumo["stock_actual"]) == Decimal("15.00")

    actualizada = _dosis(client, headers, regla["id_recordatorio"])[0]
    assert actualizada["aplicada"] is True
    assert actualizada["fecha_aplicacion"] is not None

    eventos = _eventos(db_path, camada["id_camada"])
    assert len(eventos) == 1
    assert eventos[0][0] == "vacunacion"
    assert "Newcastle" in eventos[0][1]


def test_vitamina_registra_evento_como_otro(cliente) -> None:
    """Una vitamina queda en el historial como evento 'otro'."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "apl6@test.com")
    camada = _crear_camada(client, headers)
    insumo = _crear_insumo(
        client, headers, categoria="vitamina", nombre="Vitamina B"
    )
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        id_insumo=insumo["id_insumo"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]

    respuesta = client.post(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar",
        json={},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert _eventos(db_path, camada["id_camada"])[0][0] == "otro"


def test_aplicar_dos_veces_devuelve_409(cliente) -> None:
    """Una dosis aplicada no se puede volver a registrar."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl7@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]
    url = f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar"

    primera = client.post(url, json={}, headers=headers)
    segunda = client.post(url, json={}, headers=headers)

    assert primera.status_code == 200
    assert segunda.status_code == 409


def test_aplicar_sin_stock_devuelve_400(cliente) -> None:
    """Si el inventario no alcanza, la dosis queda pendiente."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl8@test.com")
    camada = _crear_camada(client, headers)
    insumo = _crear_insumo(client, headers, stock=1)
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        id_insumo=insumo["id_insumo"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]

    respuesta = client.post(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar",
        json={},
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert "Stock insuficiente" in respuesta.json()["detail"]
    assert _dosis(client, headers, regla["id_recordatorio"])[0]["aplicada"] is False
    stock = client.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(stock["stock_actual"]) == Decimal("1.00")


def test_aplicar_con_otra_cantidad(cliente) -> None:
    """La cantidad usada puede ajustarse al aplicar la dosis."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl9@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]

    respuesta = client.post(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar",
        json={"cantidad": 2},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert Decimal(respuesta.json()["stock_resultante"]) == Decimal("18")


def test_aplicar_lote_descuenta_cada_camada(cliente) -> None:
    """El lote aplica todas las dosis del recordatorio global."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl10@test.com")
    _crear_camada(client, headers, "Camada Norte")
    _crear_camada(client, headers, "Camada Sur")
    regla = _crear_recordatorio(client, headers, fecha_fin=HOY.isoformat())
    ids = [item["id_dosis"] for item in _dosis(client, headers, regla["id_recordatorio"])]
    assert len(ids) == 2

    respuesta = client.post(
        "/api/recordatorios/dosis/aplicar-lote",
        json={"ids_dosis": ids},
        headers=headers,
    )

    assert respuesta.status_code == 200, respuesta.text
    resultados = respuesta.json()
    assert len(resultados) == 2
    assert {item["nombre_camada"] for item in resultados} == {
        "Camada Norte",
        "Camada Sur",
    }
    # El insumo se descuenta dos veces: 20 → 15 → 10.
    assert sorted(Decimal(item["stock_resultante"]) for item in resultados) == [
        Decimal("10"),
        Decimal("15"),
    ]
    for item in _dosis(client, headers, regla["id_recordatorio"]):
        assert item["aplicada"] is True


def test_lote_sin_stock_no_aplica_ninguna(cliente) -> None:
    """Si una dosis del lote falla, no se registra ninguna."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl11@test.com")
    _crear_camada(client, headers, "Camada Norte")
    _crear_camada(client, headers, "Camada Sur")
    insumo = _crear_insumo(client, headers, stock=4)
    regla = _crear_recordatorio(
        client,
        headers,
        id_insumo=insumo["id_insumo"],
        fecha_fin=HOY.isoformat(),
        cantidad=3,
    )
    ids = [item["id_dosis"] for item in _dosis(client, headers, regla["id_recordatorio"])]

    respuesta = client.post(
        "/api/recordatorios/dosis/aplicar-lote",
        json={"ids_dosis": ids},
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert all(
        item["aplicada"] is False
        for item in _dosis(client, headers, regla["id_recordatorio"])
    )
    stock = client.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(stock["stock_actual"]) == Decimal("4.00")


def test_detalle_de_la_dosis_trae_el_stock(cliente) -> None:
    """El modal obtiene insumo, unidad y stock de la dosis."""
    client, _ = cliente
    headers = _registrar_usuario(client, "apl12@test.com")
    camada = _crear_camada(client, headers)
    regla = _crear_recordatorio(
        client,
        headers,
        id_camada=camada["id_camada"],
        fecha_fin=HOY.isoformat(),
    )
    dosis = _dosis(client, headers, regla["id_recordatorio"])[0]

    respuesta = client.get(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}", headers=headers
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["nombre_insumo"] == "Newcastle"
    assert cuerpo["unidad_medida"] == "ml"
    assert Decimal(cuerpo["cantidad"]) == Decimal("5")
    assert Decimal(cuerpo["stock_actual"]) == Decimal("20.00")
    assert cuerpo["aplicada"] is False


def test_dosis_de_otro_usuario_devuelve_404(cliente) -> None:
    """Un avicultor no puede aplicar dosis ajenas."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "apl13@test.com")
    intruso = _registrar_usuario(client, "apl14@test.com")
    camada = _crear_camada(client, dueno)
    regla = _crear_recordatorio(
        client, dueno, id_camada=camada["id_camada"], fecha_fin=HOY.isoformat()
    )
    dosis = _dosis(client, dueno, regla["id_recordatorio"])[0]

    detalle = client.get(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}", headers=intruso
    )
    aplicar = client.post(
        f"/api/recordatorios/dosis/{dosis['id_dosis']}/aplicar",
        json={},
        headers=intruso,
    )

    assert detalle.status_code == 404
    assert aplicar.status_code == 404

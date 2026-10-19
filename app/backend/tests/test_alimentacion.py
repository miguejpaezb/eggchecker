"""Pruebas del registro de alimentación por camada.

Cubre el descuento de inventario, las validaciones de stock/categoría y
la entrada del consumo en el reporte de alimento.
"""

from datetime import date, datetime, timedelta
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
    categoria: str = "alimento",
    nombre: str = "Concentrado",
    stock: int = 20,
    umbral: int = 5,
) -> dict:
    """Crea un insumo de la categoría indicada."""
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": _categoria_id(cliente, headers, categoria),
            "nombre_insumo": nombre,
            "unidad_medida": "kg",
            "stock_actual": stock,
            "umbral_minimo": umbral,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def _crear_camada(cliente, headers, nombre: str = "Camada Prueba") -> dict:
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
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def _alimentar(
    cliente,
    headers,
    id_camada: int,
    id_insumo: int,
    cantidad,
    observaciones=None,
) -> object:
    """Registra una alimentación con los datos indicados."""
    return cliente.post(
        f"/api/camadas/{id_camada}/alimentacion",
        json={
            "id_insumo": id_insumo,
            "cantidad": cantidad,
            "observaciones": observaciones,
        },
        headers=headers,
    )


def _historial(cliente, headers, id_camada: int) -> list:
    """Lista el historial de alimentación de una camada."""
    respuesta = cliente.get(f"/api/camadas/{id_camada}/alimentacion", headers=headers)
    assert respuesta.status_code == 200
    return respuesta.json()


def test_registrar_descuenta_stock_y_guarda_fecha(cliente) -> None:
    """Un registro válido descuenta el insumo y guarda fecha y hora."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim1@test.com")
    insumo = _crear_insumo(client, headers, stock=20)
    camada = _crear_camada(client, headers)

    respuesta = _alimentar(
        client, headers, camada["id_camada"], insumo["id_insumo"], 5, "Mañana"
    )

    assert respuesta.status_code == 201, respuesta.text
    cuerpo = respuesta.json()
    assert cuerpo["nombre_camada"] == camada["nombre_camada"]
    assert cuerpo["nombre_insumo"] == insumo["nombre_insumo"]
    assert cuerpo["unidad_medida"] == "kg"
    assert Decimal(cuerpo["cantidad"]) == Decimal("5.00")
    assert cuerpo["observaciones"] == "Mañana"
    # La fecha y hora las pone el servidor.
    assert datetime.fromisoformat(cuerpo["fecha_hora"]).date() == HOY

    stock = client.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(stock["stock_actual"]) == Decimal("15.00")
    assert len(_historial(client, headers, camada["id_camada"])) == 1


def test_cantidad_mayor_al_stock_devuelve_400(cliente) -> None:
    """No se puede alimentar con más cantidad de la disponible."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim2@test.com")
    insumo = _crear_insumo(client, headers, stock=5)
    camada = _crear_camada(client, headers)

    respuesta = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 6)

    assert respuesta.status_code == 400
    assert "supera el stock" in respuesta.json()["detail"]
    stock = client.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(stock["stock_actual"]) == Decimal("5.00")
    assert _historial(client, headers, camada["id_camada"]) == []


def test_cantidad_cero_devuelve_422(cliente) -> None:
    """La cantidad debe ser mayor que cero."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim3@test.com")
    insumo = _crear_insumo(client, headers)
    camada = _crear_camada(client, headers)

    respuesta = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 0)

    assert respuesta.status_code == 422


def test_insumo_que_no_es_alimento_devuelve_400(cliente) -> None:
    """Solo se aceptan insumos de la categoría alimento."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim4@test.com")
    insumo = _crear_insumo(client, headers, categoria="medicamento")
    camada = _crear_camada(client, headers)

    respuesta = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 2)

    assert respuesta.status_code == 400
    assert "alimento" in respuesta.json()["detail"]
    assert _historial(client, headers, camada["id_camada"]) == []


def test_camada_retirada_devuelve_400(cliente) -> None:
    """Una camada retirada ya no se alimenta."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim5@test.com")
    insumo = _crear_insumo(client, headers)
    camada = _crear_camada(client, headers)
    descarte = client.post(
        f"/api/camadas/{camada['id_camada']}/descartar", headers=headers
    )
    assert descarte.status_code == 200

    respuesta = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 2)

    assert respuesta.status_code == 400
    assert "camadas activas" in respuesta.json()["detail"]


def test_camada_de_otro_usuario_devuelve_404(cliente) -> None:
    """No se alimenta una camada ajena."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "alim6@test.com")
    intruso = _registrar_usuario(client, "alim7@test.com")
    insumo = _crear_insumo(client, intruso)
    camada = _crear_camada(client, dueno)

    respuesta = _alimentar(client, intruso, camada["id_camada"], insumo["id_insumo"], 2)

    assert respuesta.status_code == 404


def test_insumo_de_otro_usuario_devuelve_404(cliente) -> None:
    """No se usa inventario de otro avicultor."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "alim8@test.com")
    intruso = _registrar_usuario(client, "alim9@test.com")
    insumo = _crear_insumo(client, dueno)
    camada = _crear_camada(client, intruso)

    respuesta = _alimentar(client, intruso, camada["id_camada"], insumo["id_insumo"], 2)

    assert respuesta.status_code == 404


def test_dos_registros_el_mismo_dia(cliente) -> None:
    """Una camada puede alimentarse varias veces el mismo día."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim10@test.com")
    insumo = _crear_insumo(client, headers, stock=20)
    camada = _crear_camada(client, headers)

    primera = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 3)
    segunda = _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 2)

    assert primera.status_code == 201
    assert segunda.status_code == 201
    historial = _historial(client, headers, camada["id_camada"])
    assert len(historial) == 2
    # Más reciente primero (mismo sello: se desempata por id).
    assert historial[0]["id_alimentacion"] > historial[1]["id_alimentacion"]
    stock = client.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(stock["stock_actual"]) == Decimal("15.00")


def test_alimentacion_entra_en_el_reporte(cliente) -> None:
    """El consumo de alimento aparece en el reporte de la categoría."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim11@test.com")
    insumo = _crear_insumo(client, headers)
    camada = _crear_camada(client, headers)
    _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 4)

    reporte = client.get(
        "/api/reportes/consolidado",
        params={"desde": HOY.isoformat(), "hasta": HOY.isoformat()},
        headers=headers,
    ).json()

    assert Decimal(reporte["alimento"]["kg_usados"]) == Decimal("4.00")


def test_alimentacion_bajo_el_umbral_genera_aviso(cliente) -> None:
    """Si la salida deja el insumo bajo el umbral, se avisa del stock."""
    client, _ = cliente
    headers = _registrar_usuario(client, "alim12@test.com")
    insumo = _crear_insumo(client, headers, stock=12, umbral=10)
    camada = _crear_camada(client, headers)

    _alimentar(client, headers, camada["id_camada"], insumo["id_insumo"], 5)

    avisos = client.get("/api/notificaciones", headers=headers).json()
    stock = [aviso for aviso in avisos if aviso["id_insumo"] == insumo["id_insumo"]]
    assert len(stock) == 1
    assert stock[0]["tipo"] == "stock_bajo"


def test_historial_de_camada_ajena_devuelve_404(cliente) -> None:
    """El historial solo lo ve el dueño de la camada."""
    client, _ = cliente
    dueno = _registrar_usuario(client, "alim13@test.com")
    intruso = _registrar_usuario(client, "alim14@test.com")
    camada = _crear_camada(client, dueno)

    respuesta = client.get(
        f"/api/camadas/{camada['id_camada']}/alimentacion", headers=intruso
    )

    assert respuesta.status_code == 404

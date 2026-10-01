from datetime import date
from decimal import Decimal
from pathlib import Path
from uuid import uuid4

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from app.models.camada import Camada
from app.models.stock_produccion import StockProduccion
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
EDAD_PRODUCCION = 28
TIPO_AA = 1


@pytest.fixture()
def cliente(tmp_path: Path):
    """Levanta la API con una base SQLite temporal cargada con el seed.

    Returns:
        TestClient: Cliente de prueba contra la API con BD temporal.
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
    client.session_prueba = session_prueba
    yield client
    client.close()
    engine.dispose()


def _correo_unico(prefijo: str) -> str:
    """Genera un correo único para aislar cada prueba."""
    return f"{prefijo}{uuid4().hex[:8]}@test.com"


def _registrar_usuario(cliente, prefijo: str) -> tuple[dict, int]:
    """Registra un usuario y devuelve su header y su id."""
    respuesta = cliente.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Dashboard Test",
            "correo_electronico": _correo_unico(prefijo),
            "contrasena": CLAVE_VALIDA,
        },
    )
    assert respuesta.status_code == 201
    id_usuario = respuesta.json()["id_usuario"]
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}, id_usuario


def _fijar_stock(
    cliente, id_usuario: int, id_tipo: int, cantidad: int, valor: int = 400
) -> None:
    """Crea o actualiza la fila de stock de un usuario y tipo en la BD."""
    sesion = cliente.session_prueba()
    try:
        registro = (
            sesion.query(StockProduccion)
            .filter_by(id_usuario=id_usuario, id_tipo=id_tipo)
            .first()
        )
        if registro is None:
            registro = StockProduccion(
                id_usuario=id_usuario,
                id_tipo=id_tipo,
                cantidad_actual=0,
                valor_unidad=Decimal("0.00"),
            )
            sesion.add(registro)
        registro.cantidad_actual = cantidad
        registro.valor_unidad = Decimal(valor)
        sesion.commit()
    finally:
        sesion.close()


def _crear_camada(cliente, headers: dict, cantidad_inicial: int = 100) -> int:
    """Crea una camada en etapa de producción y devuelve su id."""
    respuesta = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": f"Camada {uuid4().hex[:6]}",
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": cantidad_inicial,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    id_camada = respuesta.json()["id_camada"]
    sesion = cliente.session_prueba()
    try:
        camada = sesion.get(Camada, id_camada)
        camada.edad_semanas = EDAD_PRODUCCION
        sesion.commit()
    finally:
        sesion.close()
    return id_camada


def _registrar_produccion(
    cliente, headers: dict, id_camada: int, cantidades: dict
) -> dict:
    """Registra la recolección de hoy para una camada existente."""
    respuesta = cliente.post(
        "/api/produccion",
        json={
            "id_camada": id_camada,
            "fecha_recoleccion": date.today().isoformat(),
            "unidad": "unidad",
            **cantidades,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_cliente(cliente, headers: dict) -> dict:
    """Crea un cliente activo y devuelve su cuerpo de respuesta."""
    respuesta = cliente.post(
        "/api/clientes",
        json={
            "nombre_cliente": f"Cliente {uuid4().hex[:6]}",
            "telefono": "3001234567",
            "direccion": "Calle 1 # 2-3",
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _dashboard(cliente, headers: dict) -> dict:
    """Consulta el dashboard y devuelve su cuerpo de respuesta."""
    respuesta = cliente.get("/api/dashboard", headers=headers)
    assert respuesta.status_code == 200
    return respuesta.json()


def test_dashboard_sin_datos_devuelve_ceros(cliente) -> None:
    """Un usuario nuevo tiene el dashboard en cero y la serie en 7 días."""
    headers, _ = _registrar_usuario(cliente, "dash1")

    cuerpo = _dashboard(cliente, headers)

    assert cuerpo["produccion_hoy"] == 0
    assert cuerpo["variacion_produccion"] is None
    assert cuerpo["aves_activas"] == 0
    assert cuerpo["pedidos_pendientes"] == 0
    assert cuerpo["alertas_count"] == 0
    assert len(cuerpo["semana"]["serie"]) == 7
    assert cuerpo["semana"]["total_huevos"] == 0
    assert cuerpo["semana"]["mejor_dia"] is None
    assert cuerpo["semana"]["valor_producido"] == "0.00"
    assert cuerpo["semana"]["tasa_postura"] == "0.00"
    assert cuerpo["semana"]["mortalidad"] == 0
    assert cuerpo["alertas"] == []
    assert cuerpo["pedidos_recientes"] == []


def test_dashboard_kpis_produccion_y_postura(cliente) -> None:
    """La producción de hoy alimenta los KPI, la serie y la tasa de postura."""
    headers, _ = _registrar_usuario(cliente, "dash2")
    id_camada = _crear_camada(cliente, headers, cantidad_inicial=100)
    _registrar_produccion(
        cliente, headers, id_camada, {"aa": 20, "a": 15, "b": 5, "no_apto": 2}
    )

    cuerpo = _dashboard(cliente, headers)

    assert cuerpo["produccion_hoy"] == 42
    assert cuerpo["aves_activas"] == 100
    assert cuerpo["semana"]["total_huevos"] == 42
    # 42 / (100 * 7) * 100 = 6.00
    assert cuerpo["semana"]["tasa_postura"] == "6.00"
    ultimo = cuerpo["semana"]["serie"][-1]
    assert ultimo["fecha"] == date.today().isoformat()
    assert ultimo["total_huevos"] == 42
    assert cuerpo["semana"]["mejor_dia"]["total_huevos"] == 42


def test_dashboard_valor_producido_con_precio_referencia(cliente) -> None:
    """El valor producido usa el precio de referencia del tipo de huevo."""
    headers, id_usuario = _registrar_usuario(cliente, "dash3")
    _fijar_stock(cliente, id_usuario, TIPO_AA, 100, 400)
    id_camada = _crear_camada(cliente, headers, cantidad_inicial=100)
    _registrar_produccion(cliente, headers, id_camada, {"aa": 20})

    cuerpo = _dashboard(cliente, headers)

    assert cuerpo["semana"]["valor_producido"] == "8000.00"


def test_dashboard_alertas_de_insumos(cliente) -> None:
    """Un insumo bajo el umbral aparece en las alertas del dashboard."""
    headers, _ = _registrar_usuario(cliente, "dash4")
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": 1,
            "nombre_insumo": "Concentrado Ponedoras",
            "unidad_medida": "bulto",
            "stock_actual": 2,
            "umbral_minimo": 3,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201

    cuerpo = _dashboard(cliente, headers)

    assert cuerpo["alertas_count"] == 1
    assert cuerpo["alertas"][0]["nombre_insumo"] == "Concentrado Ponedoras"
    assert cuerpo["alertas"][0]["stock_actual"] == "2.00"


def test_dashboard_pedidos_pendientes_y_recientes(cliente) -> None:
    """Los pedidos por entregar se cuentan y se listan como recientes."""
    headers, id_usuario = _registrar_usuario(cliente, "dash5")
    _fijar_stock(cliente, id_usuario, TIPO_AA, 100, 400)
    id_cliente = _crear_cliente(cliente, headers)["id_cliente"]
    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={
            "id_cliente": id_cliente,
            "detalles": [{"id_tipo": TIPO_AA, "cantidad": 10, "precio_unitario": 400}],
        },
        headers=headers,
    )
    assert respuesta.status_code == 201

    cuerpo = _dashboard(cliente, headers)

    assert cuerpo["pedidos_pendientes"] == 1
    assert cuerpo["pedidos_recientes"][0]["descripcion"] == "10 AA"
    assert cuerpo["pedidos_recientes"][0]["estado_pedido"] == "pendiente"
    assert cuerpo["pedidos_recientes"][0]["fecha_pedido"] == date.today().isoformat()


def test_dashboard_requiere_autenticacion(cliente) -> None:
    """Sin token, el dashboard responde 401."""
    respuesta = cliente.get("/api/dashboard")

    assert respuesta.status_code == 401

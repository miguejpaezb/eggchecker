from datetime import date, timedelta
from decimal import Decimal
from pathlib import Path
from uuid import uuid4

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from app.models.camada import Camada
from app.models.evento_sanitario import EventoSanitario
from app.models.stock_produccion import StockProduccion
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
EDAD_PRODUCCION = 28


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
            "nombre_completo": "Reportes Test",
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


def _crear_pedido(cliente, headers: dict, id_cliente: int, detalles: list) -> dict:
    """Crea un pedido y devuelve su cuerpo de respuesta."""
    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={"id_cliente": id_cliente, "detalles": detalles},
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_insumo(
    cliente, headers: dict, costo: int, stock: int = 100, categoria: int = 1
) -> dict:
    """Crea un insumo con costo unitario y stock inicial."""
    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": categoria,
            "nombre_insumo": f"Insumo {uuid4().hex[:6]}",
            "unidad_medida": "kg",
            "stock_actual": stock,
            "umbral_minimo": 10,
            "costo_unitario": costo,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_camada(cliente, headers: dict, cantidad_inicial: int = 100) -> int:
    """Crea una camada y devuelve su identificador."""
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
    return respuesta.json()["id_camada"]


def _editar_camada(
    cliente, id_camada: int, fecha_ingreso: date | None = None, edad: int | None = None
) -> None:
    """Ajusta por sesión la fecha de ingreso y/o la edad de una camada."""
    sesion = cliente.session_prueba()
    try:
        camada = sesion.get(Camada, id_camada)
        if fecha_ingreso is not None:
            camada.fecha_ingreso = fecha_ingreso
        if edad is not None:
            camada.edad_semanas = edad
        sesion.commit()
    finally:
        sesion.close()


def _evento(
    cliente,
    id_camada: int,
    tipo: str,
    descripcion: str,
    mortalidad: int = 0,
    fecha: date | None = None,
):
    """Inserta un evento sanitario directamente para las pruebas."""
    sesion = cliente.session_prueba()
    try:
        sesion.add(
            EventoSanitario(
                id_camada=id_camada,
                tipo_evento=tipo,
                fecha_evento=fecha or date.today(),
                descripcion=descripcion,
                mortalidad=mortalidad,
            )
        )
        sesion.commit()
    finally:
        sesion.close()


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


def _consolidado(cliente, headers: dict, **params) -> dict:
    """Consulta el consolidado del día actual y devuelve su cuerpo."""
    consulta = {
        "desde": date.today().isoformat(),
        "hasta": date.today().isoformat(),
        **params,
    }
    respuesta = cliente.get(
        "/api/reportes/consolidado", params=consulta, headers=headers
    )
    assert respuesta.status_code == 200
    return respuesta.json()


def test_consolidado_sin_datos_devuelve_ceros(cliente) -> None:
    """Un usuario nuevo tiene consolidado vacío y ganancia en cero."""
    headers, _ = _registrar_usuario(cliente, "rep1")

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["produccion"]["total_huevos"] == 0
    assert cuerpo["produccion"]["cubetas_completas"] == 0
    assert cuerpo["ventas"]["ingreso_total"] == "0.00"
    assert cuerpo["ventas"]["num_pedidos"] == 0
    assert cuerpo["gastos"]["total"] == "0.00"
    assert cuerpo["ganancia"]["valor"] == "0.00"
    assert cuerpo["ganancia"]["porcentaje"] == "0.00"
    assert cuerpo["salud"]["gallinas_perdidas"] == 0
    assert cuerpo["salud"]["causa_principal"] is None
    assert cuerpo["salud"]["vacunacion_al_dia"] is False
    assert cuerpo["alimento"]["kg_usados"] == "0.00"
    assert cuerpo["inventario"]["valor_stock"] == "0.00"
    assert cuerpo["camada"] is None


def test_consolidado_agrupa_produccion_por_tipo_y_cubetas(cliente) -> None:
    """La producción se refleja por tipo, cubetas y no aptos."""
    headers, _ = _registrar_usuario(cliente, "rep2")
    id_camada = _crear_camada(cliente, headers)
    _editar_camada(cliente, id_camada, edad=EDAD_PRODUCCION)
    _registrar_produccion(
        cliente, headers, id_camada, {"aa": 20, "a": 15, "b": 5, "no_apto": 2}
    )

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["produccion"]["total_huevos"] == 42
    assert cuerpo["produccion"]["cubetas_completas"] == 1
    assert cuerpo["produccion"]["huevos_no_aptos"] == 2
    cubetas = {
        item["nombre_tipo"]: item["cubetas"]
        for item in cuerpo["produccion"]["cubetas_por_tipo"]
    }
    assert cubetas == {"AA": 0, "A": 0, "B": 0}
    assert cuerpo["produccion"]["serie"][0]["total_huevos"] == 42


def test_consolidado_ventas_gastos_y_ganancia(cliente) -> None:
    """Ventas, gasto de insumos y ganancia se calculan con el período."""
    headers, id_usuario = _registrar_usuario(cliente, "rep3")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    id_cliente = _crear_cliente(cliente, headers)["id_cliente"]
    _crear_pedido(
        cliente,
        headers,
        id_cliente,
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )
    insumo = _crear_insumo(cliente, headers, costo=500, stock=100)
    consumo = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "salida", "cantidad": 20},
        headers=headers,
    )
    assert consumo.status_code == 201

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["ventas"]["ingreso_total"] == "12000.00"
    assert cuerpo["ventas"]["num_pedidos"] == 1
    assert cuerpo["ventas"]["unidades_totales"] == 30
    assert cuerpo["gastos"]["total"] == "10000.00"
    assert cuerpo["gastos"]["por_insumo"][0]["costo_unitario"] == "500.00"
    assert cuerpo["ganancia"]["valor"] == "2000.00"
    assert cuerpo["ganancia"]["porcentaje"] == "16.67"


def test_consolidado_excluye_pedidos_cancelados(cliente) -> None:
    """Un pedido cancelado no suma ingresos ni pedidos."""
    headers, id_usuario = _registrar_usuario(cliente, "rep4")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    id_cliente = _crear_cliente(cliente, headers)["id_cliente"]
    pedido = _crear_pedido(
        cliente,
        headers,
        id_cliente,
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )
    cancelado = cliente.post(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/cancelar", headers=headers
    )
    assert cancelado.status_code == 200

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["ventas"]["ingreso_total"] == "0.00"
    assert cuerpo["ventas"]["num_pedidos"] == 0


def test_costo_promedio_ponderado_en_entradas(cliente) -> None:
    """Una entrada con costo recalcula el promedio ponderado del insumo."""
    headers, _ = _registrar_usuario(cliente, "rep5")
    insumo = _crear_insumo(cliente, headers, costo=500, stock=100)

    entrada = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={
            "tipo_movimiento": "entrada",
            "cantidad": 10,
            "costo_unitario": 650,
        },
        headers=headers,
    )
    assert entrada.status_code == 201

    actualizado = cliente.get(
        f"/api/insumos/{insumo['id_insumo']}", headers=headers
    ).json()
    # (100 * 500 + 10 * 650) / 110 = 513.636... -> 513.64
    assert Decimal(actualizado["costo_unitario"]) == Decimal("513.64")


def test_consolidado_gastos_usa_costo_promedio(cliente) -> None:
    """La salida se valoriza al costo promedio vigente tras la compra."""
    headers, _ = _registrar_usuario(cliente, "rep6")
    insumo = _crear_insumo(cliente, headers, costo=500, stock=100)
    url = f"/api/insumos/{insumo['id_insumo']}/movimientos"
    cliente.post(
        url,
        json={"tipo_movimiento": "entrada", "cantidad": 10, "costo_unitario": 650},
        headers=headers,
    )
    salida = cliente.post(
        url,
        json={"tipo_movimiento": "salida", "cantidad": 5},
        headers=headers,
    )
    assert salida.status_code == 201

    cuerpo = _consolidado(cliente, headers)

    # 5 * 513.64 = 2568.20
    assert cuerpo["gastos"]["total"] == "2568.20"


def test_consolidado_salud_y_bajas(cliente) -> None:
    """Gallinas perdidas, causa principal y vacunación del período."""
    headers, _ = _registrar_usuario(cliente, "rep7")
    id_camada = _crear_camada(cliente, headers)
    _evento(cliente, id_camada, "mortalidad", "Moquillo", mortalidad=2)
    _evento(cliente, id_camada, "vacunacion", "Newcastle")

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["salud"]["gallinas_perdidas"] == 2
    assert cuerpo["salud"]["causa_principal"] == "Moquillo"
    assert cuerpo["salud"]["vacunacion_al_dia"] is True


def test_consolidado_uso_de_comida(cliente) -> None:
    """Solo el alimento suma a kg usados y a su promedio diario."""
    headers, _ = _registrar_usuario(cliente, "rep8")
    alimento = _crear_insumo(cliente, headers, costo=500, stock=100, categoria=1)
    medicamento = _crear_insumo(cliente, headers, costo=300, stock=50, categoria=2)
    for insumo, cantidad in ((alimento, 2.5), (medicamento, 4)):
        respuesta = cliente.post(
            f"/api/insumos/{insumo['id_insumo']}/movimientos",
            json={"tipo_movimiento": "salida", "cantidad": cantidad},
            headers=headers,
        )
        assert respuesta.status_code == 201

    cuerpo = _consolidado(cliente, headers)

    assert cuerpo["alimento"]["kg_usados"] == "2.50"
    assert cuerpo["alimento"]["promedio_diario"] == "2.50"
    assert len(cuerpo["alimento"]["por_insumo"]) == 1


def test_consolidado_reporte_por_camada(cliente) -> None:
    """El modo camada acota la producción y añade su ficha."""
    headers, _ = _registrar_usuario(cliente, "rep9")
    id_camada = _crear_camada(cliente, headers, cantidad_inicial=100)
    _editar_camada(
        cliente,
        id_camada,
        fecha_ingreso=date.today() - timedelta(weeks=20),
        edad=36,
    )
    _evento(
        cliente,
        id_camada,
        "mortalidad",
        "Moquillo",
        mortalidad=3,
        fecha=date.today() - timedelta(weeks=10),
    )
    _registrar_produccion(
        cliente, headers, id_camada, {"aa": 30, "a": 20, "b": 10, "no_apto": 2}
    )

    cuerpo = _consolidado(cliente, headers, camada=id_camada)

    assert cuerpo["produccion"]["total_huevos"] == 62
    ficha = cuerpo["camada"]
    assert ficha["id_camada"] == id_camada
    assert ficha["aves_semana_28"] == 97
    assert ficha["edad_semanas"] == 36
    assert ficha["total_huevos"] == 62
    assert ficha["cubetas_completas"] == 2
    assert ficha["promedio_diario"] == 62.0


def test_consolidado_camada_ajena_devuelve_404(cliente) -> None:
    """Una camada de otro usuario responde 404."""
    dueno, _ = _registrar_usuario(cliente, "rep10")
    id_camada = _crear_camada(cliente, dueno)
    intruso, _ = _registrar_usuario(cliente, "rep11")

    respuesta = cliente.get(
        "/api/reportes/consolidado",
        params={"camada": id_camada},
        headers=intruso,
    )

    assert respuesta.status_code == 404


def test_consolidado_rango_invalido_devuelve_400(cliente) -> None:
    """Una fecha inicial posterior a la final responde 400."""
    headers, _ = _registrar_usuario(cliente, "rep12")

    respuesta = cliente.get(
        "/api/reportes/consolidado",
        params={"desde": "2026-03-31", "hasta": "2026-03-01"},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_consolidado_agrupacion_invalida_devuelve_422(cliente) -> None:
    """Una agrupación distinta de dia/semana/mes no pasa la validación."""
    headers, _ = _registrar_usuario(cliente, "rep13")

    respuesta = cliente.get(
        "/api/reportes/consolidado",
        params={"agrupacion": "hora"},
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_exportar_pdf_devuelve_pdf_no_vacio(cliente) -> None:
    """El endpoint de exportación responde un PDF válido y no vacío."""
    headers, id_usuario = _registrar_usuario(cliente, "rep14")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    id_cliente = _crear_cliente(cliente, headers)["id_cliente"]
    _crear_pedido(
        cliente,
        headers,
        id_cliente,
        [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
    )

    respuesta = cliente.get(
        "/api/reportes/pdf",
        params={
            "desde": date.today().isoformat(),
            "hasta": date.today().isoformat(),
        },
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"] == "application/pdf"
    assert respuesta.content.startswith(b"%PDF")
    assert len(respuesta.content) > 1000


def test_reportes_requiere_autenticacion(cliente) -> None:
    """Sin token, el consolidado responde 401."""
    respuesta = cliente.get("/api/reportes/consolidado")

    assert respuesta.status_code == 401

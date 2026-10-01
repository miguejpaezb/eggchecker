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
            "nombre_completo": "Vendedor Test",
            "correo_electronico": _correo_unico(prefijo),
            "contrasena": CLAVE_VALIDA,
        },
    )
    assert respuesta.status_code == 201
    id_usuario = respuesta.json()["id_usuario"]
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}, id_usuario


def _headers_de(cliente, id_usuario: int) -> dict:
    """Devuelve el header Authorization para un usuario existente."""
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}


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


def _crear_cliente(cliente, headers: dict, **reemplazos) -> dict:
    """Crea un cliente activo y devuelve su cuerpo de respuesta."""
    datos = {
        "nombre_cliente": f"Cliente {uuid4().hex[:6]}",
        "telefono": "3001234567",
        "direccion": "Calle 1 # 2-3",
    }
    datos.update(reemplazos)
    respuesta = cliente.post("/api/clientes", json=datos, headers=headers)
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


def _stock_por_tipo(cliente, headers: dict) -> dict:
    """Devuelve el stock del usuario indexado por nombre de tipo."""
    respuesta = cliente.get("/api/ventas/stock", headers=headers)
    assert respuesta.status_code == 200
    return {fila["nombre_tipo"]: fila for fila in respuesta.json()["por_tipo"]}


def test_stock_inicial_del_seed(cliente) -> None:
    """El usuario 1 arranca con 10 B; No_apto no cuenta en el stock."""
    headers = _headers_de(cliente, 1)

    respuesta = cliente.get("/api/ventas/stock", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    por_tipo = {fila["nombre_tipo"]: fila for fila in cuerpo["por_tipo"]}
    assert "No_apto" not in por_tipo
    assert por_tipo["B"]["cantidad_actual"] == 10
    assert cuerpo["total_disponible"] == 10
    assert Decimal(por_tipo["AA"]["valor_unidad"]) == Decimal("400.00")


def test_crear_pedido_descuenta_stock_y_calcula_total(cliente) -> None:
    """Un pedido de 30 AA a 400 deja stock 70 y total 12000."""
    headers, id_usuario = _registrar_usuario(cliente, "ven1")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)

    cuerpo = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )

    assert cuerpo["estado_pedido"] == "pendiente"
    assert cuerpo["fecha_pedido"] == date.today().isoformat()
    assert cuerpo["unidades_totales"] == 30
    assert Decimal(cuerpo["valor_total"]) == Decimal("12000.00")
    assert cuerpo["detalles"][0]["nombre_tipo"] == "AA"
    assert Decimal(cuerpo["detalles"][0]["subtotal"]) == Decimal("12000.00")

    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 70


def test_crear_pedido_sin_stock_suficiente_devuelve_400(cliente) -> None:
    """Pedir más huevos de los disponibles responde 400."""
    headers, id_usuario = _registrar_usuario(cliente, "ven2")
    _fijar_stock(cliente, id_usuario, 1, 5, 400)
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={
            "id_cliente": creado["id_cliente"],
            "detalles": [{"id_tipo": 1, "cantidad": 6, "precio_unitario": 400}],
        },
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert "Stock insuficiente" in respuesta.json()["detail"]


def test_crear_pedido_cantidad_invalida_devuelve_422(cliente) -> None:
    """Cantidad cero, negativa o decimal no pasa la validación."""
    headers, id_usuario = _registrar_usuario(cliente, "ven3")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)

    for cantidad in (0, -5, 1.5):
        respuesta = cliente.post(
            "/api/ventas/pedidos",
            json={
                "id_cliente": creado["id_cliente"],
                "detalles": [
                    {"id_tipo": 1, "cantidad": cantidad, "precio_unitario": 400}
                ],
            },
            headers=headers,
        )
        assert respuesta.status_code == 422


def test_crear_pedido_cliente_suspendido_devuelve_400(cliente) -> None:
    """No se puede vender a un cliente suspendido."""
    headers, id_usuario = _registrar_usuario(cliente, "ven4")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    cliente.post(f"/api/clientes/{creado['id_cliente']}/suspender", headers=headers)

    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={
            "id_cliente": creado["id_cliente"],
            "detalles": [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
        },
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_crear_pedido_cliente_ajeno_devuelve_404(cliente) -> None:
    """Un cliente de otro usuario responde 404."""
    dueno, _ = _registrar_usuario(cliente, "ven5")
    creado = _crear_cliente(cliente, dueno)
    intruso, id_intruso = _registrar_usuario(cliente, "ven6")
    _fijar_stock(cliente, id_intruso, 1, 100, 400)

    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={
            "id_cliente": creado["id_cliente"],
            "detalles": [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
        },
        headers=intruso,
    )

    assert respuesta.status_code == 404


def test_crear_pedido_actualiza_fecha_ultima_compra(cliente) -> None:
    """Al vender, el cliente queda con la fecha de hoy como última compra."""
    headers, id_usuario = _registrar_usuario(cliente, "ven7")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)

    _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
    )

    detalle = cliente.get(
        f"/api/clientes/{creado['id_cliente']}", headers=headers
    ).json()
    assert detalle["fecha_ultima_compra"] == date.today().isoformat()


def test_transiciones_de_estado_del_pedido(cliente) -> None:
    """pendiente→enviado→recibido funciona; los retrocesos no."""
    headers, id_usuario = _registrar_usuario(cliente, "ven8")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
    )
    url = f"/api/ventas/pedidos/{pedido['id_pedido']}/estado"

    enviado = cliente.patch(url, json={"estado": "enviado"}, headers=headers)
    repetido = cliente.patch(url, json={"estado": "enviado"}, headers=headers)
    recibido = cliente.patch(url, json={"estado": "recibido"}, headers=headers)
    retroceso = cliente.patch(url, json={"estado": "enviado"}, headers=headers)

    assert enviado.status_code == 200
    assert enviado.json()["estado_pedido"] == "enviado"
    assert repetido.status_code == 400
    assert recibido.status_code == 200
    assert recibido.json()["estado_pedido"] == "recibido"
    assert retroceso.status_code == 400


def test_editar_pedido_ajusta_stock_y_total(cliente) -> None:
    """Bajar la cantidad de 30 a 10 devuelve 20 al stock."""
    headers, id_usuario = _registrar_usuario(cliente, "ven9")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )

    respuesta = cliente.patch(
        f"/api/ventas/pedidos/{pedido['id_pedido']}",
        json={"detalles": [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 500}]},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert Decimal(respuesta.json()["valor_total"]) == Decimal("5000.00")
    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 90
    assert Decimal(stock["AA"]["valor_unidad"]) == Decimal("500.00")


def test_no_editar_pedido_enviado_devuelve_400(cliente) -> None:
    """Un pedido en camino ya no se puede editar."""
    headers, id_usuario = _registrar_usuario(cliente, "ven10")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
    )
    cliente.patch(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/estado",
        json={"estado": "enviado"},
        headers=headers,
    )

    respuesta = cliente.patch(
        f"/api/ventas/pedidos/{pedido['id_pedido']}",
        json={"detalles": [{"id_tipo": 1, "cantidad": 5, "precio_unitario": 400}]},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_cancelar_pedido_repone_stock(cliente) -> None:
    """Cancelar un pedido devuelve los huevos al stock y queda registrado."""
    headers, id_usuario = _registrar_usuario(cliente, "ven11")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )

    respuesta = cliente.post(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/cancelar", headers=headers
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["estado_pedido"] == "cancelado"
    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 100


def test_eliminar_pedido_con_contrasena_repone_y_borra(cliente) -> None:
    """Eliminar con la contraseña correcta borra el pedido y repone stock."""
    headers, id_usuario = _registrar_usuario(cliente, "ven12")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )

    respuesta = cliente.post(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/eliminar",
        json={"contrasena": CLAVE_VALIDA},
        headers=headers,
    )

    assert respuesta.status_code == 204
    assert (
        cliente.get(
            f"/api/ventas/pedidos/{pedido['id_pedido']}", headers=headers
        ).status_code
        == 404
    )
    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 100


def test_eliminar_pedido_con_contrasena_incorrecta_401(cliente) -> None:
    """Con contraseña incorrecta no se borra ni repone el stock."""
    headers, id_usuario = _registrar_usuario(cliente, "ven13")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 30, "precio_unitario": 400}],
    )

    respuesta = cliente.post(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/eliminar",
        json={"contrasena": "Incorrecta1!"},
        headers=headers,
    )

    assert respuesta.status_code == 401
    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 70


def test_no_eliminar_pedido_recibido_devuelve_400(cliente) -> None:
    """Un pedido recibido no se puede eliminar."""
    headers, id_usuario = _registrar_usuario(cliente, "ven14")
    _fijar_stock(cliente, id_usuario, 1, 100, 400)
    creado = _crear_cliente(cliente, headers)
    pedido = _crear_pedido(
        cliente,
        headers,
        creado["id_cliente"],
        [{"id_tipo": 1, "cantidad": 10, "precio_unitario": 400}],
    )
    cliente.patch(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/estado",
        json={"estado": "recibido"},
        headers=headers,
    )

    respuesta = cliente.post(
        f"/api/ventas/pedidos/{pedido['id_pedido']}/eliminar",
        json={"contrasena": CLAVE_VALIDA},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_produccion_incrementa_el_stock(cliente) -> None:
    """Registrar producción suma los huevos recolectados al stock."""
    headers, _ = _registrar_usuario(cliente, "ven15")
    creado = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": f"Camada {uuid4().hex[:6]}",
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    assert creado.status_code == 201
    id_camada = creado.json()["id_camada"]

    sesion = cliente.session_prueba()
    try:
        camada = sesion.get(Camada, id_camada)
        camada.edad_semanas = EDAD_PRODUCCION
        sesion.commit()
    finally:
        sesion.close()

    produccion = cliente.post(
        "/api/produccion",
        json={
            "id_camada": id_camada,
            "fecha_recoleccion": date.today().isoformat(),
            "unidad": "unidad",
            "aa": 20,
            "a": 15,
            "b": 5,
            "no_apto": 2,
        },
        headers=headers,
    )
    assert produccion.status_code == 201

    stock = _stock_por_tipo(cliente, headers)
    assert stock["AA"]["cantidad_actual"] == 20
    assert stock["A"]["cantidad_actual"] == 15
    assert stock["B"]["cantidad_actual"] == 5
    assert "No_apto" not in stock


def test_crear_pedido_con_no_apto_devuelve_400(cliente) -> None:
    """El tipo No_apto no se puede incluir en un pedido."""
    headers, id_usuario = _registrar_usuario(cliente, "ven16")
    _fijar_stock(cliente, id_usuario, 4, 100, 100)
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.post(
        "/api/ventas/pedidos",
        json={
            "id_cliente": creado["id_cliente"],
            "detalles": [{"id_tipo": 4, "cantidad": 5, "precio_unitario": 100}],
        },
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["detail"] == "Los huevos No_apto no se pueden vender"

from decimal import Decimal
from pathlib import Path
from uuid import uuid4

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"


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
    yield client
    client.close()
    engine.dispose()


def _correo_unico(prefijo: str) -> str:
    """Genera un correo único para aislar cada prueba."""
    return f"{prefijo}{uuid4().hex[:8]}@test.com"


def _registrar_usuario(cliente, prefijo: str) -> dict:
    """Registra un usuario y devuelve su header de autorización.

    Args:
        cliente: Cliente de prueba.
        prefijo: Prefijo único del correo del usuario a registrar.

    Returns:
        dict: Header Authorization con el token del usuario creado.
    """
    respuesta = cliente.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": _correo_unico(prefijo),
            "contrasena": CLAVE_VALIDA,
        },
    )
    assert respuesta.status_code == 201
    id_usuario = respuesta.json()["id_usuario"]
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}


def _crear_categoria(cliente, headers: dict, nombre: str | None = None) -> dict:
    """Crea una categoría de prueba y devuelve su cuerpo de respuesta."""
    respuesta = cliente.post(
        "/api/categorias-insumo",
        json={
            "nombre_categ": nombre or f"categoria_{uuid4().hex[:6]}",
            "descripcion": "Categoría de prueba",
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _crear_insumo(cliente, headers: dict, categoria_id: int, **reemplazos) -> dict:
    """Crea un insumo válido, con sobreescrituras opcionales.

    Args:
        cliente: Cliente de prueba.
        headers: Header Authorization del usuario dueño.
        categoria_id: Categoría a la que pertenece el insumo.
        **reemplazos: Campos a reemplazar en el payload base.

    Returns:
        dict: Cuerpo de respuesta del insumo creado.
    """
    datos = {
        "id_categoria": categoria_id,
        "nombre_insumo": f"insumo_{uuid4().hex[:6]}",
        "unidad_medida": "kg",
        "stock_actual": 50,
        "umbral_minimo": 10,
    }
    datos.update(reemplazos)
    respuesta = cliente.post("/api/insumos", json=datos, headers=headers)
    assert respuesta.status_code == 201
    return respuesta.json()


def test_crear_categoria_y_duplicada_devuelve_409(cliente) -> None:
    """Crear una categoría responde 201 y duplicar el nombre responde 409."""
    headers = _registrar_usuario(cliente, "cat1")
    nombre = f"cat_{uuid4().hex[:6]}"

    primera = cliente.post(
        "/api/categorias-insumo", json={"nombre_categ": nombre}, headers=headers
    )
    duplicada = cliente.post(
        "/api/categorias-insumo", json={"nombre_categ": nombre}, headers=headers
    )

    assert primera.status_code == 201
    assert duplicada.status_code == 409
    assert duplicada.json()["detail"] == "La categoría ya existe"


def test_duplicar_categoria_del_seed_devuelve_409(cliente) -> None:
    """Usar un nombre del catálogo fijo (alimento) responde 409."""
    headers = _registrar_usuario(cliente, "cat2")

    respuesta = cliente.post(
        "/api/categorias-insumo", json={"nombre_categ": "alimento"}, headers=headers
    )

    assert respuesta.status_code == 409


def test_listar_categorias_incluye_el_catalogo_global(cliente) -> None:
    """Cualquier usuario autenticado ve el catálogo global de categorías."""
    headers = _registrar_usuario(cliente, "cat3")
    creada = _crear_categoria(cliente, headers)

    respuesta = cliente.get("/api/categorias-insumo", headers=headers)

    assert respuesta.status_code == 200
    nombres = [c["nombre_categ"] for c in respuesta.json()]
    assert creada["nombre_categ"] in nombres
    assert "alimento" in nombres


def test_eliminar_categoria_con_insumos_devuelve_409(cliente) -> None:
    """Una categoría que ya tiene insumos asociados no se puede eliminar."""
    headers = _registrar_usuario(cliente, "cat4")
    categoria = _crear_categoria(cliente, headers)
    _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.delete(
        f"/api/categorias-insumo/{categoria['id_categoria']}", headers=headers
    )

    assert respuesta.status_code == 409
    assert respuesta.json()["detail"] == "No se puede eliminar: tiene insumos asociados"


def test_eliminar_categoria_sin_insumos_devuelve_204(cliente) -> None:
    """Una categoría sin insumos se elimina y responde 204."""
    headers = _registrar_usuario(cliente, "cat5")
    categoria = _crear_categoria(cliente, headers)

    respuesta = cliente.delete(
        f"/api/categorias-insumo/{categoria['id_categoria']}", headers=headers
    )

    assert respuesta.status_code == 204


def test_crear_insumo_responde_201_con_stock_y_umbral(cliente) -> None:
    """Crear un insumo responde 201 con stock y umbral correctos."""
    headers = _registrar_usuario(cliente, "ins1")
    categoria = _crear_categoria(cliente, headers)

    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": categoria["id_categoria"],
            "nombre_insumo": "Concentrado Prueba",
            "unidad_medida": "kg",
            "stock_actual": 50,
            "umbral_minimo": 10,
        },
        headers=headers,
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["activo"] is True
    assert Decimal(cuerpo["stock_actual"]) == Decimal("50.00")
    assert Decimal(cuerpo["umbral_minimo"]) == Decimal("10.00")


def test_crear_insumo_con_categoria_inexistente_devuelve_404(cliente) -> None:
    """Crear un insumo en una categoría que no existe responde 404."""
    headers = _registrar_usuario(cliente, "ins2")

    respuesta = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": 9999,
            "nombre_insumo": "Insumo Huérfano",
            "unidad_medida": "kg",
            "stock_actual": 10,
            "umbral_minimo": 5,
        },
        headers=headers,
    )

    assert respuesta.status_code == 404
    assert respuesta.json()["detail"] == "Categoría no encontrada"


def test_crear_insumo_con_stock_o_umbral_cero_devuelve_422(cliente) -> None:
    """Ni el stock actual ni el umbral mínimo pueden ser cero."""
    headers = _registrar_usuario(cliente, "ins0")
    categoria = _crear_categoria(cliente, headers)

    sin_stock = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": categoria["id_categoria"],
            "nombre_insumo": "Sin stock",
            "unidad_medida": "kg",
            "stock_actual": 0,
            "umbral_minimo": 5,
        },
        headers=headers,
    )
    sin_umbral = cliente.post(
        "/api/insumos",
        json={
            "id_categoria": categoria["id_categoria"],
            "nombre_insumo": "Sin umbral",
            "unidad_medida": "kg",
            "stock_actual": 5,
            "umbral_minimo": 0,
        },
        headers=headers,
    )

    assert sin_stock.status_code == 422
    assert sin_umbral.status_code == 422


def test_editar_insumo_con_campo_prohibido_devuelve_422(cliente) -> None:
    """Editar no permite cambiar categoría, stock ni activo."""
    headers = _registrar_usuario(cliente, "ins0b")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.patch(
        f"/api/insumos/{insumo['id_insumo']}",
        json={"stock_actual": 999},
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_editar_insumo_nombre_unidad_y_umbral(cliente) -> None:
    """PATCH actualiza nombre, unidad de medida y umbral mínimo."""
    headers = _registrar_usuario(cliente, "ins0c")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.patch(
        f"/api/insumos/{insumo['id_insumo']}",
        json={
            "nombre_insumo": "Renombrado",
            "unidad_medida": "litro",
            "umbral_minimo": 7,
        },
        headers=headers,
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["nombre_insumo"] == "Renombrado"
    assert cuerpo["unidad_medida"] == "litro"
    assert Decimal(cuerpo["umbral_minimo"]) == Decimal("7.00")


def test_listar_insumos_aisla_por_usuario(cliente) -> None:
    """Un usuario solo ve sus propios insumos; los ajenos devuelven 404."""
    dueno = _registrar_usuario(cliente, "ins3")
    categoria = _crear_categoria(cliente, dueno)
    insumo = _crear_insumo(cliente, dueno, categoria["id_categoria"])
    intruso = _registrar_usuario(cliente, "ins4")

    lista_intruso = cliente.get("/api/insumos", headers=intruso)
    ajeno = cliente.get(f"/api/insumos/{insumo['id_insumo']}", headers=intruso)

    assert lista_intruso.status_code == 200
    assert lista_intruso.json() == []
    assert ajeno.status_code == 404


def test_listar_insumos_filtra_por_categoria(cliente) -> None:
    """GET /insumos?categoria= devuelve solo los insumos de esa categoría."""
    headers = _registrar_usuario(cliente, "ins5")
    cat_a = _crear_categoria(cliente, headers)
    cat_b = _crear_categoria(cliente, headers)
    en_a = _crear_insumo(cliente, headers, cat_a["id_categoria"])
    _crear_insumo(cliente, headers, cat_b["id_categoria"])

    respuesta = cliente.get(
        f"/api/insumos?categoria={cat_a['id_categoria']}", headers=headers
    )

    assert respuesta.status_code == 200
    assert [i["id_insumo"] for i in respuesta.json()] == [en_a["id_insumo"]]


def test_movimiento_entrada_aumenta_stock(cliente) -> None:
    """Una entrada de 20 sobre un stock de 50 deja el stock en 70."""
    headers = _registrar_usuario(cliente, "ins6")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "entrada", "cantidad": 20},
        headers=headers,
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["tipo_movimiento"] == "entrada"
    assert Decimal(cuerpo["stock_resultante"]) == Decimal("70.00")

    detalle = cliente.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    assert Decimal(detalle["stock_actual"]) == Decimal("70.00")


def test_movimiento_salida_disminuye_stock(cliente) -> None:
    """Una salida de 20 sobre un stock de 50 deja el stock en 30."""
    headers = _registrar_usuario(cliente, "ins7")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "salida", "cantidad": 20},
        headers=headers,
    )

    assert respuesta.status_code == 201
    assert Decimal(respuesta.json()["stock_resultante"]) == Decimal("30.00")


def test_salida_que_excede_stock_devuelve_400(cliente) -> None:
    """Una salida mayor al stock actual responde 400 stock insuficiente."""
    headers = _registrar_usuario(cliente, "ins8")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "salida", "cantidad": 9999},
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["detail"] == "Stock insuficiente para registrar la salida"


def test_salida_fallida_no_cambia_stock_ni_deja_movimiento(cliente) -> None:
    """Tras una salida rechazada, el stock no cambia y no queda movimiento."""
    headers = _registrar_usuario(cliente, "ins9")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "salida", "cantidad": 9999},
        headers=headers,
    )
    assert respuesta.status_code == 400

    detalle = cliente.get(f"/api/insumos/{insumo['id_insumo']}", headers=headers).json()
    historial = cliente.get(
        f"/api/insumos/{insumo['id_insumo']}/movimientos", headers=headers
    ).json()

    assert Decimal(detalle["stock_actual"]) == Decimal("50.00")
    assert historial == []


def test_historial_ordena_mas_reciente_primero(cliente) -> None:
    """El historial devuelve los movimientos del más reciente al más antiguo."""
    headers = _registrar_usuario(cliente, "ins10")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    for cantidad, obs in [(10, "primera"), (5, "segunda"), (7, "tercera")]:
        cliente.post(
            f"/api/insumos/{insumo['id_insumo']}/movimientos",
            json={
                "tipo_movimiento": "entrada",
                "cantidad": cantidad,
                "observaciones": obs,
            },
            headers=headers,
        )

    respuesta = cliente.get(
        f"/api/insumos/{insumo['id_insumo']}/movimientos", headers=headers
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert [m["observaciones"] for m in cuerpo] == ["tercera", "segunda", "primera"]
    assert [Decimal(m["cantidad"]) for m in cuerpo] == [
        Decimal("7.00"),
        Decimal("5.00"),
        Decimal("10.00"),
    ]


def test_alertas_incluyen_insumo_bajo_umbral_con_deficit(cliente) -> None:
    """Un insumo con stock menor al umbral aparece en alertas con su déficit."""
    headers = _registrar_usuario(cliente, "ins11")
    categoria = _crear_categoria(cliente, headers)
    en_alerta = _crear_insumo(
        cliente, headers, categoria["id_categoria"], stock_actual=5, umbral_minimo=10
    )
    _crear_insumo(
        cliente, headers, categoria["id_categoria"], stock_actual=50, umbral_minimo=10
    )

    respuesta = cliente.get("/api/insumos/alertas", headers=headers)

    assert respuesta.status_code == 200
    alertas = respuesta.json()
    assert [a["id_insumo"] for a in alertas] == [en_alerta["id_insumo"]]
    assert Decimal(alertas[0]["deficit"]) == Decimal("5.00")


def test_alertas_excluyen_insumo_suspendido(cliente) -> None:
    """Un insumo bajo umbral pero suspendido no aparece en alertas."""
    headers = _registrar_usuario(cliente, "ins12")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(
        cliente, headers, categoria["id_categoria"], stock_actual=5, umbral_minimo=10
    )

    suspendido = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/suspender", headers=headers
    )
    assert suspendido.status_code == 200
    assert suspendido.json()["activo"] is False

    respuesta = cliente.get("/api/insumos/alertas", headers=headers)

    assert respuesta.status_code == 200
    assert respuesta.json() == []


def test_suspender_y_activar_insumo(cliente) -> None:
    """Suspender lo deja inactivo y activar lo reactiva."""
    headers = _registrar_usuario(cliente, "ins13")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    cliente.post(f"/api/insumos/{insumo['id_insumo']}/suspender", headers=headers)
    activos = cliente.get("/api/insumos", headers=headers).json()
    con_suspendidos = cliente.get("/api/insumos?activo=false", headers=headers).json()
    assert [i["id_insumo"] for i in activos] == []
    assert [i["id_insumo"] for i in con_suspendidos] == [insumo["id_insumo"]]

    reactivar = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/activar", headers=headers
    )
    assert reactivar.status_code == 200
    assert reactivar.json()["activo"] is True


def test_suspendido_no_permite_movimiento_ni_edicion(cliente) -> None:
    """Un insumo suspendido no se puede usar ni manipular."""
    headers = _registrar_usuario(cliente, "ins14")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])
    cliente.post(f"/api/insumos/{insumo['id_insumo']}/suspender", headers=headers)

    movimiento = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/movimientos",
        json={"tipo_movimiento": "entrada", "cantidad": 5},
        headers=headers,
    )
    edicion = cliente.patch(
        f"/api/insumos/{insumo['id_insumo']}",
        json={"nombre_insumo": "No editable"},
        headers=headers,
    )

    assert movimiento.status_code == 400
    assert edicion.status_code == 400


def test_descontinuar_oculta_el_insumo_y_no_se_reactiva(cliente) -> None:
    """Descontinuar lo oculta del listado y no se puede activar."""
    headers = _registrar_usuario(cliente, "ins15")
    categoria = _crear_categoria(cliente, headers)
    insumo = _crear_insumo(cliente, headers, categoria["id_categoria"])

    respuesta = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/descontinuar", headers=headers
    )
    assert respuesta.status_code == 200
    assert respuesta.json()["descontinuado"] is True
    assert respuesta.json()["activo"] is False

    lista = cliente.get("/api/insumos?activo=false", headers=headers).json()
    assert lista == []

    reactivar = cliente.post(
        f"/api/insumos/{insumo['id_insumo']}/activar", headers=headers
    )
    assert reactivar.status_code == 400

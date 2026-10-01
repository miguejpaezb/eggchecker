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


def _crear_cliente(cliente, headers: dict, **reemplazos) -> dict:
    """Crea un cliente válido, con sobreescrituras opcionales.

    Args:
        cliente: Cliente de prueba.
        headers: Header Authorization del usuario dueño.
        **reemplazos: Campos a reemplazar en el payload base.

    Returns:
        dict: Cuerpo de respuesta del cliente creado.
    """
    datos = {
        "nombre_cliente": f"cliente_{uuid4().hex[:6]}",
        "telefono": "3001234567",
        "direccion": "Calle 1 # 2-3",
    }
    datos.update(reemplazos)
    respuesta = cliente.post("/api/clientes", json=datos, headers=headers)
    assert respuesta.status_code == 201
    return respuesta.json()


def test_crear_cliente_responde_201_activo_y_sin_coordenadas(cliente) -> None:
    """Crear un cliente responde 201 con activo en True y sin coordenadas."""
    headers = _registrar_usuario(cliente, "cli1")

    respuesta = cliente.post(
        "/api/clientes",
        json={
            "nombre_cliente": "Tienda Nueva",
            "telefono": "3001234567",
            "direccion": "Cra 10 # 20-30",
        },
        headers=headers,
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["activo"] is True
    assert cuerpo["nombre_cliente"] == "Tienda Nueva"
    assert cuerpo["fecha_ultima_compra"] is None
    assert "latitud" not in cuerpo
    assert "longitud" not in cuerpo


def test_crear_cliente_sin_telefono_ni_direccion(cliente) -> None:
    """Teléfono y dirección son opcionales."""
    headers = _registrar_usuario(cliente, "cli2")

    respuesta = cliente.post(
        "/api/clientes", json={"nombre_cliente": "Solo Nombre"}, headers=headers
    )

    assert respuesta.status_code == 201
    assert respuesta.json()["telefono"] is None
    assert respuesta.json()["direccion"] is None


def test_crear_cliente_que_supera_el_limite_del_plan_devuelve_400(cliente) -> None:
    """El plan gratuito permite 10 clientes; el décimo primero responde 400."""
    headers = _registrar_usuario(cliente, "cli3")

    for _ in range(10):
        _crear_cliente(cliente, headers)

    respuesta = cliente.post(
        "/api/clientes",
        json={"nombre_cliente": "Cliente Extra"},
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["detail"] == "Límite de clientes del plan alcanzado"


def test_listar_clientes_aisla_por_usuario(cliente) -> None:
    """Un usuario solo ve sus propios clientes; los ajenos devuelven 404."""
    dueno = _registrar_usuario(cliente, "cli4")
    creado = _crear_cliente(cliente, dueno)
    intruso = _registrar_usuario(cliente, "cli5")

    lista_intruso = cliente.get("/api/clientes", headers=intruso)
    ajeno = cliente.get(f"/api/clientes/{creado['id_cliente']}", headers=intruso)

    assert lista_intruso.status_code == 200
    assert lista_intruso.json() == []
    assert ajeno.status_code == 404


def test_editar_cliente_cambia_nombre_telefono_y_direccion(cliente) -> None:
    """PATCH actualiza nombre, teléfono y dirección."""
    headers = _registrar_usuario(cliente, "cli6")
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.patch(
        f"/api/clientes/{creado['id_cliente']}",
        json={
            "nombre_cliente": "Renombrado",
            "telefono": "3109998877",
            "direccion": "Nueva dirección 4-5",
        },
        headers=headers,
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["nombre_cliente"] == "Renombrado"
    assert cuerpo["telefono"] == "3109998877"
    assert cuerpo["direccion"] == "Nueva dirección 4-5"


def test_editar_cliente_con_campo_prohibido_devuelve_422(cliente) -> None:
    """Editar no permite cambiar el estado ni el usuario."""
    headers = _registrar_usuario(cliente, "cli7")
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.patch(
        f"/api/clientes/{creado['id_cliente']}",
        json={"activo": False},
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_suspender_activar_y_filtro_de_activos(cliente) -> None:
    """Suspender lo oculta del listado activo y activar lo reactiva."""
    headers = _registrar_usuario(cliente, "cli8")
    creado = _crear_cliente(cliente, headers)

    suspendido = cliente.post(
        f"/api/clientes/{creado['id_cliente']}/suspender", headers=headers
    )
    assert suspendido.status_code == 200
    assert suspendido.json()["activo"] is False

    activos = cliente.get("/api/clientes", headers=headers).json()
    con_suspendidos = cliente.get("/api/clientes?activo=false", headers=headers).json()
    assert activos == []
    assert [c["id_cliente"] for c in con_suspendidos] == [creado["id_cliente"]]

    reactivado = cliente.post(
        f"/api/clientes/{creado['id_cliente']}/activar", headers=headers
    )
    assert reactivado.status_code == 200
    assert reactivado.json()["activo"] is True


def test_eliminar_cliente_con_contrasena_incorrecta_devuelve_401(cliente) -> None:
    """Con contraseña incorrecta no se borra y responde 401."""
    headers = _registrar_usuario(cliente, "cli9")
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.post(
        f"/api/clientes/{creado['id_cliente']}/eliminar",
        json={"contrasena": "Incorrecta1!"},
        headers=headers,
    )

    assert respuesta.status_code == 401
    assert respuesta.json()["detail"] == "Contraseña incorrecta"

    sigue = cliente.get(f"/api/clientes/{creado['id_cliente']}", headers=headers)
    assert sigue.status_code == 200


def test_eliminar_cliente_con_contrasena_correcta_devuelve_204(cliente) -> None:
    """Con la contraseña correcta el cliente se borra y responde 204."""
    headers = _registrar_usuario(cliente, "cli10")
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.post(
        f"/api/clientes/{creado['id_cliente']}/eliminar",
        json={"contrasena": CLAVE_VALIDA},
        headers=headers,
    )

    assert respuesta.status_code == 204

    desaparecido = cliente.get(f"/api/clientes/{creado['id_cliente']}", headers=headers)
    assert desaparecido.status_code == 404


def test_crear_cliente_con_telefono_no_numerico_devuelve_422(cliente) -> None:
    """Un teléfono con letras, símbolos o espacios no es válido."""
    headers = _registrar_usuario(cliente, "cli11")

    for telefono in ("300abc4567", "300-123-4567", "300 123 4567"):
        respuesta = cliente.post(
            "/api/clientes",
            json={"nombre_cliente": "Teléfono inválido", "telefono": telefono},
            headers=headers,
        )
        assert respuesta.status_code == 422


def test_crear_cliente_acepta_telefonos_solo_digitos(cliente) -> None:
    """El teléfono admite cualquier longitud mientras sean solo dígitos."""
    headers = _registrar_usuario(cliente, "cli12")

    corto = _crear_cliente(cliente, headers, telefono="6012345")
    largo = _crear_cliente(cliente, headers, telefono="3001234567")
    vacio = _crear_cliente(cliente, headers, telefono="")

    assert corto["telefono"] == "6012345"
    assert largo["telefono"] == "3001234567"
    assert vacio["telefono"] is None


def test_editar_cliente_con_telefono_no_numerico_devuelve_422(cliente) -> None:
    """Editar con un teléfono inválido responde 422 y no cambia nada."""
    headers = _registrar_usuario(cliente, "cli13")
    creado = _crear_cliente(cliente, headers)

    respuesta = cliente.patch(
        f"/api/clientes/{creado['id_cliente']}",
        json={"telefono": "no-es-numero"},
        headers=headers,
    )

    assert respuesta.status_code == 422

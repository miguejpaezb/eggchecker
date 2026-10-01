from datetime import date, timedelta
from pathlib import Path
from uuid import uuid4

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from app.models.camada import Camada
from app.models.produccion_detalle import ProduccionDetalle
from app.models.produccion_diaria import ProduccionDiaria
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine, text
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
    """Registra un usuario y devuelve su header y su id.

    Args:
        cliente: Cliente de prueba.
        prefijo: Prefijo único del correo del usuario a registrar.

    Returns:
        tuple[dict, int]: Header Authorization y id del usuario creado.
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
    return {"Authorization": f"Bearer {token}"}, id_usuario


def _headers_de(cliente, id_usuario: int) -> dict:
    """Devuelve el header Authorization para un usuario existente."""
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}


def _crear_camada(cliente, headers: dict, cantidad: int = 100) -> dict:
    """Crea una camada de prueba y devuelve su cuerpo de respuesta."""
    respuesta = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": f"Camada {uuid4().hex[:6]}",
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": cantidad,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _fijar_edad(cliente, id_camada: int, edad: int) -> None:
    """Fija la edad en semanas de una camada directamente en la BD."""
    sesion = cliente.session_prueba()
    try:
        camada = sesion.get(Camada, id_camada)
        camada.edad_semanas = edad
        sesion.commit()
    finally:
        sesion.close()


def _camada_en_produccion(cliente, headers: dict, cantidad: int = 100) -> dict:
    """Crea una camada activa en etapa de producción (28 semanas)."""
    camada = _crear_camada(cliente, headers, cantidad=cantidad)
    _fijar_edad(cliente, camada["id_camada"], EDAD_PRODUCCION)
    return camada


def _payload_produccion(id_camada: int, **reemplazos) -> dict:
    """Devuelve un payload válido de producción con sobreescrituras."""
    datos = {
        "id_camada": id_camada,
        "fecha_recoleccion": date.today().isoformat(),
        "unidad": "unidad",
        "aa": 20,
        "a": 15,
        "b": 5,
        "no_apto": 2,
    }
    datos.update(reemplazos)
    return datos


def _registrar_produccion(cliente, headers: dict, id_camada: int, **reemplazos) -> dict:
    """Registra una producción y devuelve el JSON de la respuesta."""
    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(id_camada, **reemplazos),
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _insertar_produccion(
    cliente, id_usuario: int, id_camada: int, fecha: date, total: int
) -> None:
    """Inserta una producción histórica directamente en la BD."""
    sesion = cliente.session_prueba()
    try:
        produccion = ProduccionDiaria(
            id_usuario=id_usuario,
            id_camada=id_camada,
            fecha_recoleccion=fecha,
            total_huevos=total,
        )
        sesion.add(produccion)
        sesion.flush()
        sesion.add(
            ProduccionDetalle(
                id_produccion=produccion.id_produccion,
                id_tipo=1,
                cantidad=total,
            )
        )
        sesion.commit()
    finally:
        sesion.close()


def _insertar_venta(
    cliente, id_usuario: int, id_tipo: int, cantidad: int, estado: str
) -> None:
    """Inserta un pedido con su detalle por SQL directo (aún sin ORM)."""
    sesion = cliente.session_prueba()
    try:
        resultado_cliente = sesion.execute(
            text(
                "INSERT INTO cliente (id_usuario, nombre_cliente, activo) "
                "VALUES (:u, 'Cliente Test', 1)"
            ),
            {"u": id_usuario},
        )
        id_cliente = resultado_cliente.lastrowid
        resultado_pedido = sesion.execute(
            text(
                "INSERT INTO pedido "
                "(id_cliente, id_usuario, fecha_pedido, estado_pedido, "
                "valor_total) VALUES (:c, :u, :f, :e, 0)"
            ),
            {
                "c": id_cliente,
                "u": id_usuario,
                "f": date.today().isoformat(),
                "e": estado,
            },
        )
        id_pedido = resultado_pedido.lastrowid
        sesion.execute(
            text(
                "INSERT INTO detalle_pedido "
                "(id_pedido, id_tipo, cantidad, precio_unitario) "
                "VALUES (:p, :t, :q, 1)"
            ),
            {"p": id_pedido, "t": id_tipo, "q": cantidad},
        )
        sesion.commit()
    finally:
        sesion.close()


def test_registrar_produccion_calcula_total_en_servidor(cliente) -> None:
    """Registrar AA=20, A=15, B=5, No_apto=2 responde 201 con total 42."""
    headers, _ = _registrar_usuario(cliente, "prod1")
    camada = _camada_en_produccion(cliente, headers)

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"]),
        headers=headers,
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["total_huevos"] == 42
    assert len(cuerpo["detalle"]) == 4


def test_registrar_produccion_sin_token_devuelve_401(cliente) -> None:
    """Registrar producción sin token responde 401."""
    respuesta = cliente.post("/api/produccion", json=_payload_produccion(1))

    assert respuesta.status_code == 401


def test_registrar_produccion_mismo_dia_actualiza_el_registro(cliente) -> None:
    """Repetir el registro del día actualiza, no crea uno nuevo."""
    headers, _ = _registrar_usuario(cliente, "prod2")
    camada = _camada_en_produccion(cliente, headers)
    _registrar_produccion(cliente, headers, camada["id_camada"])

    segunda = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], aa=1, a=1, b=0, no_apto=0),
        headers=headers,
    )

    assert segunda.status_code == 201
    assert segunda.json()["total_huevos"] == 2
    lista = cliente.get(
        f"/api/produccion?camada={camada['id_camada']}", headers=headers
    ).json()
    assert len(lista) == 1
    assert lista[0]["total_huevos"] == 2
    detalle = cliente.get(
        f"/api/produccion/{lista[0]['id_produccion']}", headers=headers
    ).json()
    assert {d["nombre_tipo"] for d in detalle["detalle"]} == {"AA", "A"}


def test_registrar_produccion_en_cubetas_multiplica_por_30(cliente) -> None:
    """1 cubeta = 30 huevos: AA=1 y A=1 con unidad cubeta dan total 60."""
    headers, _ = _registrar_usuario(cliente, "prod3")
    camada = _camada_en_produccion(cliente, headers)

    cuerpo = _registrar_produccion(
        cliente,
        headers,
        camada["id_camada"],
        unidad="cubeta",
        aa=1,
        a=1,
        b=0,
        no_apto=0,
    )

    assert cuerpo["total_huevos"] == 60
    assert len(cuerpo["detalle"]) == 2
    assert {d["nombre_tipo"] for d in cuerpo["detalle"]} == {"AA", "A"}


def test_registrar_produccion_todo_cero_devuelve_400(cliente) -> None:
    """Todas las cantidades en cero responde 400."""
    headers, _ = _registrar_usuario(cliente, "prod4")
    camada = _camada_en_produccion(cliente, headers)

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], aa=0, a=0, b=0, no_apto=0),
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["detail"] == "Debe registrar al menos un tipo de huevo"


def test_registrar_produccion_cantidad_negativa_devuelve_422(cliente) -> None:
    """Una cantidad negativa no pasa la validación de Pydantic."""
    headers, _ = _registrar_usuario(cliente, "prod5")
    camada = _camada_en_produccion(cliente, headers)

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], aa=-1),
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_registrar_produccion_fecha_pasada_devuelve_400(cliente) -> None:
    """No se puede registrar producción de días anteriores."""
    headers, _ = _registrar_usuario(cliente, "prodpasado")
    camada = _camada_en_produccion(cliente, headers)
    ayer = (date.today() - timedelta(days=1)).isoformat()

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], fecha_recoleccion=ayer),
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert (
        respuesta.json()["detail"]
        == "Solo se puede registrar producción del día actual"
    )


def test_registrar_produccion_fecha_futura_devuelve_400(cliente) -> None:
    """No se puede registrar producción de días futuros."""
    headers, _ = _registrar_usuario(cliente, "prodfuturo")
    camada = _camada_en_produccion(cliente, headers)
    manana = (date.today() + timedelta(days=1)).isoformat()

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], fecha_recoleccion=manana),
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_registrar_produccion_supera_aves_actuales_devuelve_400(cliente) -> None:
    """El total no puede superar las aves actuales de la camada."""
    headers, _ = _registrar_usuario(cliente, "prodaves")
    camada = _camada_en_produccion(cliente, headers, cantidad=100)

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"], aa=101),
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert (
        respuesta.json()["detail"]
        == "El total no puede superar las aves actuales de la camada"
    )


def test_registrar_produccion_camada_fuera_de_etapa_devuelve_400(cliente) -> None:
    """Una camada de menos de 28 semanas no está en etapa de producción."""
    headers, _ = _registrar_usuario(cliente, "prodjoven")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"]),
        headers=headers,
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["detail"] == "La camada no está en etapa de producción"


def test_registrar_produccion_camada_de_otro_usuario_devuelve_404(cliente) -> None:
    """No se puede registrar producción sobre una camada ajena (404)."""
    dueno, _ = _registrar_usuario(cliente, "prod6")
    camada = _camada_en_produccion(cliente, dueno)
    intruso, _ = _registrar_usuario(cliente, "prod7")

    respuesta = cliente.post(
        "/api/produccion",
        json=_payload_produccion(camada["id_camada"]),
        headers=intruso,
    )

    assert respuesta.status_code == 404
    assert respuesta.json()["detail"] == "Camada no encontrada"


def test_listar_produccion_filtra_por_fecha(cliente) -> None:
    """GET /produccion?fecha= devuelve solo las producciones de ese día."""
    headers, id_usuario = _registrar_usuario(cliente, "prod8")
    camada = _camada_en_produccion(cliente, headers)
    hoy = date.today()
    ayer = hoy - timedelta(days=1)
    _registrar_produccion(cliente, headers, camada["id_camada"])
    _insertar_produccion(cliente, id_usuario, camada["id_camada"], ayer, 10)

    del_hoy = cliente.get(
        f"/api/produccion?fecha={hoy.isoformat()}", headers=headers
    ).json()
    del_ayer = cliente.get(
        f"/api/produccion?fecha={ayer.isoformat()}", headers=headers
    ).json()

    assert len(del_hoy) == 1
    assert del_hoy[0]["fecha_recoleccion"] == hoy.isoformat()
    assert len(del_ayer) == 1
    assert del_ayer[0]["fecha_recoleccion"] == ayer.isoformat()


def test_detalle_incluye_nombre_tipo(cliente) -> None:
    """El detalle viene con una fila por tipo y su nombre_tipo."""
    headers, _ = _registrar_usuario(cliente, "prod9")
    camada = _camada_en_produccion(cliente, headers)

    cuerpo = _registrar_produccion(cliente, headers, camada["id_camada"])

    detalle = {d["nombre_tipo"]: d["cantidad"] for d in cuerpo["detalle"]}
    assert detalle == {"AA": 20, "A": 15, "B": 5, "No_apto": 2}

    consulta = cliente.get(
        f"/api/produccion/{cuerpo['id_produccion']}", headers=headers
    )
    assert consulta.status_code == 200
    assert len(consulta.json()["detalle"]) == 4


def test_obtener_produccion_de_otro_usuario_devuelve_404(cliente) -> None:
    """Una producción ajena responde 404."""
    dueno, _ = _registrar_usuario(cliente, "prod10")
    camada = _camada_en_produccion(cliente, dueno)
    produccion = _registrar_produccion(cliente, dueno, camada["id_camada"])
    intruso, _ = _registrar_usuario(cliente, "prod11")

    respuesta = cliente.get(
        f"/api/produccion/{produccion['id_produccion']}", headers=intruso
    )

    assert respuesta.status_code == 404


def test_resumen_acumula_dia_semana_y_mes(cliente) -> None:
    """El resumen suma hoy, la semana y el mes, y desglosa el mes por tipo."""
    headers, _ = _registrar_usuario(cliente, "prod12")
    camada_a = _camada_en_produccion(cliente, headers)
    camada_b = _camada_en_produccion(cliente, headers)
    _registrar_produccion(
        cliente, headers, camada_a["id_camada"], aa=10, a=0, b=0, no_apto=0
    )
    _registrar_produccion(
        cliente, headers, camada_b["id_camada"], aa=5, a=0, b=0, no_apto=0
    )

    respuesta = cliente.get("/api/produccion/resumen", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["total_hoy"] == 15
    assert cuerpo["total_semana"] == 15
    assert cuerpo["total_mes"] == 15
    assert cuerpo["por_tipo"] == {"AA": 15, "A": 0, "B": 0, "No_apto": 0}


def test_disponibles_resta_lo_vendido(cliente) -> None:
    """Disponible = producido - vendido por tipo (40 - 10 = 30)."""
    headers, id_usuario = _registrar_usuario(cliente, "prod13")
    camada = _camada_en_produccion(cliente, headers)
    _registrar_produccion(
        cliente, headers, camada["id_camada"], aa=40, a=0, b=0, no_apto=0
    )
    _insertar_venta(cliente, id_usuario, id_tipo=1, cantidad=10, estado="entregado")

    respuesta = cliente.get("/api/huevos/disponibles", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    por_tipo = {fila["nombre_tipo"]: fila for fila in cuerpo["por_tipo"]}
    assert por_tipo["AA"]["producido"] == 40
    assert por_tipo["AA"]["vendido"] == 10
    assert por_tipo["AA"]["disponible"] == 30
    assert cuerpo["total_disponible"] == 30


def test_disponibles_no_baja_de_cero_con_datos_del_seed(cliente) -> None:
    """Con el seed, el usuario 1 vendió más de lo producido en AA y A.

    Producido (38 AA, 30 A, 10 B, 4 No_apto) menos vendido (176 AA, 100 A)
    deja disponibles 0, 0, 10 y 4.
    """
    headers = _headers_de(cliente, 1)

    respuesta = cliente.get("/api/huevos/disponibles", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    disponible = {
        fila["nombre_tipo"]: fila["disponible"] for fila in cuerpo["por_tipo"]
    }
    assert disponible == {"AA": 0, "A": 0, "B": 10, "No_apto": 4}
    assert cuerpo["total_disponible"] == 14

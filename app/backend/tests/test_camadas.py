from datetime import date, timedelta
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


@pytest.fixture()
def cliente(tmp_path: Path):
    """Levanta la API con una base SQLite temporal cargada con el seed.

    Returns:
        tuple[TestClient, Path]: Cliente de prueba y ruta de la BD temporal.
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
    """Registra un usuario y devuelve su header de autorización.

    Args:
        cliente: Cliente de prueba.
        correo: Correo único del usuario a registrar.

    Returns:
        dict: Header Authorization con el token del usuario creado.
    """
    respuesta = cliente.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": correo,
            "contrasena": CLAVE_VALIDA,
        },
    )
    assert respuesta.status_code == 201
    id_usuario = respuesta.json()["id_usuario"]
    token = crear_token_acceso(sub=str(id_usuario))
    return {"Authorization": f"Bearer {token}"}


def _datos_camada(**reemplazos) -> dict:
    """Devuelve un payload válido de camada, con sobreescrituras opcionales.

    Args:
        **reemplazos: Campos a reemplazar en el payload base.

    Returns:
        dict: Payload de creación de camada.
    """
    datos = {
        "nombre_camada": "Camada Prueba",
        "fecha_ingreso": "2026-01-01",
        "cantidad_inicial": 100,
    }
    datos.update(reemplazos)
    return datos


def test_crear_camada_responde_201_con_cantidades_iguales(cliente) -> None:
    """Crear una camada responde 201 con cantidad_actual igual a la inicial."""
    headers = _registrar_usuario(cliente, "camada1@test.com")

    respuesta = cliente.post("/api/camadas", json=_datos_camada(), headers=headers)

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["cantidad_actual"] == cuerpo["cantidad_inicial"] == 100
    assert cuerpo["estado"] == "activa"


def test_crear_camada_sin_token_devuelve_401(cliente) -> None:
    """Crear una camada sin token responde 401."""
    respuesta = cliente.post("/api/camadas", json=_datos_camada())

    assert respuesta.status_code == 401


def test_crear_camada_con_cantidad_inicial_cero_devuelve_422(cliente) -> None:
    """Una camada con cantidad_inicial 0 no pasa la validación de Pydantic."""
    headers = _registrar_usuario(cliente, "camada2@test.com")

    respuesta = cliente.post(
        "/api/camadas",
        json=_datos_camada(cantidad_inicial=0),
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_crear_camada_supera_limite_del_plan_gratuito_devuelve_400(cliente) -> None:
    """Un plan gratuito (300 aves) no admite superar su límite de aves."""
    headers = _registrar_usuario(cliente, "camada3@test.com")
    primera = cliente.post(
        "/api/camadas",
        json=_datos_camada(nombre_camada="Llenar cupo", cantidad_inicial=300),
        headers=headers,
    )
    assert primera.status_code == 201

    respuesta = cliente.post(
        "/api/camadas",
        json=_datos_camada(nombre_camada="Sobrepasa", cantidad_inicial=1),
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_listar_camadas_filtra_por_estado(cliente) -> None:
    """GET /camadas devuelve solo las camadas del usuario con ese estado."""
    headers = _registrar_usuario(cliente, "camada4@test.com")
    activa = cliente.post(
        "/api/camadas",
        json=_datos_camada(nombre_camada="Activa Uno"),
        headers=headers,
    ).json()
    retirada = cliente.post(
        "/api/camadas",
        json=_datos_camada(
            nombre_camada="Retirada Uno",
            estado="retirada",
        ),
        headers=headers,
    ).json()

    solo_activas = cliente.get("/api/camadas?estado=activa", headers=headers).json()
    solo_retiradas = cliente.get("/api/camadas?estado=retirada", headers=headers).json()
    todas = cliente.get("/api/camadas", headers=headers).json()

    assert [c["id_camada"] for c in solo_activas] == [activa["id_camada"]]
    assert [c["id_camada"] for c in solo_retiradas] == [retirada["id_camada"]]
    assert len(todas) == 2


def test_obtener_camada_de_otro_usuario_devuelve_404(cliente) -> None:
    """Una camada ajena no es visible para otro usuario (404)."""
    dueno = _registrar_usuario(cliente, "camada5@test.com")
    camada = cliente.post("/api/camadas", json=_datos_camada(), headers=dueno).json()
    intruso = _registrar_usuario(cliente, "camada6@test.com")

    respuesta = cliente.get(f"/api/camadas/{camada['id_camada']}", headers=intruso)

    assert respuesta.status_code == 404


def test_actualizar_camada_valida_cantidad_actual(cliente) -> None:
    """Actualizar una camada responde 400 si la cantidad actual excede la inicial."""
    headers = _registrar_usuario(cliente, "camada7@test.com")
    camada = cliente.post(
        "/api/camadas",
        json=_datos_camada(cantidad_inicial=100),
        headers=headers,
    ).json()

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_actual": 150},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_actualizar_camada_con_campos_validos(cliente) -> None:
    """PATCH con valores válidos actualiza el nombre y la cantidad actual."""
    headers = _registrar_usuario(cliente, "camada8@test.com")
    camada = cliente.post(
        "/api/camadas",
        json=_datos_camada(nombre_camada="Original", cantidad_inicial=100),
        headers=headers,
    ).json()

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"nombre_camada": "Renombrada", "cantidad_actual": 80},
        headers=headers,
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["nombre_camada"] == "Renombrada"
    assert cuerpo["cantidad_actual"] == 80


def test_mortalidad_valida_descuenta_cantidad_actual(cliente) -> None:
    """Registrar mortalidad responde 200 y descuenta la cantidad actual."""
    headers = _registrar_usuario(cliente, "camada9@test.com")
    camada = cliente.post(
        "/api/camadas",
        json=_datos_camada(cantidad_inicial=100),
        headers=headers,
    ).json()

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 10},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["cantidad_actual"] == 90


def test_mortalidad_que_excede_cantidad_actual_devuelve_400(cliente) -> None:
    """Una mortalidad mayor a la cantidad actual responde 400."""
    headers = _registrar_usuario(cliente, "camada10@test.com")
    camada = cliente.post(
        "/api/camadas",
        json=_datos_camada(cantidad_inicial=100),
        headers=headers,
    ).json()

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 101},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_edad_y_retiro_estimado(cliente) -> None:
    """La edad en días y el retiro estimado se calculan desde el ingreso."""
    headers = _registrar_usuario(cliente, "camada11@test.com")
    hace_30_dias = date.today() - timedelta(days=30)
    camada = cliente.post(
        "/api/camadas",
        json=_datos_camada(fecha_ingreso=hace_30_dias.isoformat()),
        headers=headers,
    ).json()

    respuesta = cliente.get(f"/api/camadas/{camada['id_camada']}/edad", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["edad_dias"] == 30
    assert (
        cuerpo["fecha_retiro_estimada"]
        == (hace_30_dias + timedelta(days=504)).isoformat()
    )

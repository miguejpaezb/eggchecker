from pathlib import Path

import pytest
from app.core.database import get_db
from app.main import create_app
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine, text
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
    yield client, db_path
    client.close()
    engine.dispose()


def test_registro_crea_usuario_sin_hash(cliente) -> None:
    """El registro responde 201 y nunca expone el hash de la contraseña."""
    client, _ = cliente

    respuesta = client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": CLAVE_VALIDA,
            "plan_suscripcion": "premium",
        },
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert "contrasena_hash" not in cuerpo
    assert cuerpo["correo_electronico"] == "ana@test.com"
    assert cuerpo["plan_suscripcion"] == "premium"


def test_registro_con_correo_duplicado_devuelve_409(cliente) -> None:
    """Registrar dos veces el mismo correo responde 409."""
    client, _ = cliente
    datos = {
        "nombre_completo": "Ana Test",
        "correo_electronico": "ana@test.com",
        "contrasena": CLAVE_VALIDA,
    }

    primera = client.post("/api/auth/registro", json=datos)
    segunda = client.post("/api/auth/registro", json=datos)

    assert primera.status_code == 201
    assert segunda.status_code == 409


def test_registro_con_contrasena_debil_devuelve_422(cliente) -> None:
    """Una contraseña sin mayúscula, número y símbolo responde 422."""
    client, _ = cliente

    respuesta = client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": "solo-minusculas-1",
        },
    )

    assert respuesta.status_code == 422


@pytest.mark.parametrize(
    "contrasena",
    [
        " Test1234! ",  # espacios al inicio y al final
        "        ",  # solo espacios
        "Test 1234!",  # espacio interno
    ],
)
def test_registro_con_espacios_en_contrasena_devuelve_422(
    cliente, contrasena: str
) -> None:
    """Una contraseña con espacios (en cualquier posición) responde 422."""
    client, _ = cliente

    respuesta = client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": contrasena,
        },
    )

    assert respuesta.status_code == 422


def test_login_correcto_devuelve_token(cliente) -> None:
    """Un login válido responde 200 con un access_token Bearer."""
    client, _ = cliente
    client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": CLAVE_VALIDA,
        },
    )

    respuesta = client.post(
        "/api/auth/login",
        json={"correo_electronico": "ana@test.com", "contrasena": CLAVE_VALIDA},
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert "access_token" in cuerpo
    assert cuerpo["token_type"] == "bearer"


def test_login_con_contrasena_incorrecta_devuelve_401(cliente) -> None:
    """Un login con contraseña equivocada responde 401."""
    client, _ = cliente
    client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": CLAVE_VALIDA,
        },
    )

    respuesta = client.post(
        "/api/auth/login",
        json={"correo_electronico": "ana@test.com", "contrasena": "Otra123!"},
    )

    assert respuesta.status_code == 401


def test_login_de_usuario_inactivo_devuelve_401(cliente) -> None:
    """Un usuario desactivado no puede iniciar sesión."""
    client, db_path = cliente
    client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": CLAVE_VALIDA,
        },
    )

    motor = create_engine(f"sqlite:///{db_path}")
    sesion_fabrica = sessionmaker(bind=motor)
    sesion = sesion_fabrica()
    sesion.execute(
        text(
            "UPDATE usuario SET activo = 0 " "WHERE correo_electronico = 'ana@test.com'"
        )
    )
    sesion.commit()
    sesion.close()
    motor.dispose()

    respuesta = client.post(
        "/api/auth/login",
        json={"correo_electronico": "ana@test.com", "contrasena": CLAVE_VALIDA},
    )

    assert respuesta.status_code == 401


def test_recuperar_correo_registrado_devuelve_mensaje_sin_token(cliente) -> None:
    """La recuperación de un correo registrado responde 200 sin exponer token."""
    client, _ = cliente

    respuesta = client.post(
        "/api/auth/recuperar",
        json={"correo_electronico": "lucia.torres@gmail.com"},
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert "mensaje" in cuerpo
    assert "token_recuperacion" not in cuerpo


def test_recuperar_correo_inexistente_devuelve_404(cliente) -> None:
    """Un correo no registrado responde 404 y corta el flujo."""
    client, _ = cliente

    respuesta = client.post(
        "/api/auth/recuperar",
        json={"correo_electronico": "noexiste@test.com"},
    )

    assert respuesta.status_code == 404


def test_recuperar_guarda_token_hasheado(cliente) -> None:
    """La solicitud guarda solo el hash del token (64 chars), no el crudo."""
    client, db_path = cliente
    client.post(
        "/api/auth/recuperar",
        json={"correo_electronico": "lucia.torres@gmail.com"},
    )

    motor = create_engine(f"sqlite:///{db_path}")
    sesion_fabrica = sessionmaker(bind=motor)
    with sesion_fabrica() as sesion:
        id_esperado = sesion.execute(
            text(
                "SELECT id_usuario FROM usuario "
                "WHERE correo_electronico = 'lucia.torres@gmail.com'"
            )
        ).scalar()
        fila = sesion.execute(
            text("SELECT id_usuario, token_hash, usado FROM token_recuperacion")
        ).fetchone()
    motor.dispose()

    assert fila is not None
    assert fila.id_usuario == id_esperado
    assert len(fila.token_hash) == 64
    assert fila.usado == 0


def test_recuperar_invalida_token_previo(cliente) -> None:
    """Pedir un segundo token invalida el anterior: solo queda uno vigente."""
    client, db_path = cliente
    datos = {"correo_electronico": "lucia.torres@gmail.com"}
    client.post("/api/auth/recuperar", json=datos)
    client.post("/api/auth/recuperar", json=datos)

    motor = create_engine(f"sqlite:///{db_path}")
    sesion_fabrica = sessionmaker(bind=motor)
    with sesion_fabrica() as sesion:
        total = sesion.execute(text("SELECT COUNT(*) FROM token_recuperacion")).scalar()
        vigentes = sesion.execute(
            text("SELECT COUNT(*) FROM token_recuperacion WHERE usado = 0")
        ).scalar()
    motor.dispose()

    assert total == 2
    assert vigentes == 1


def test_perfil_sin_token_devuelve_401(cliente) -> None:
    """Consultar el perfil sin token responde 401."""
    client, _ = cliente

    respuesta = client.get("/api/usuarios/me")

    assert respuesta.status_code == 401


def test_perfil_con_token_devuelve_limites_del_plan(cliente) -> None:
    """El perfil autenticado trae los límites coherentes con el plan."""
    client, _ = cliente
    client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "ana@test.com",
            "contrasena": CLAVE_VALIDA,
        },
    )
    login = client.post(
        "/api/auth/login",
        json={"correo_electronico": "ana@test.com", "contrasena": CLAVE_VALIDA},
    ).json()
    token = login["access_token"]

    respuesta = client.get(
        "/api/usuarios/me", headers={"Authorization": f"Bearer {token}"}
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["plan_suscripcion"] == "gratuito"
    assert cuerpo["aves_max"] == 300
    assert cuerpo["clientes_max"] == 10
    assert cuerpo["aves_actuales"] == 0
    assert cuerpo["clientes_actuales"] == 0

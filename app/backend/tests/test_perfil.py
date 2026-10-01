from datetime import date
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


def _registrar_y_token(client: TestClient, correo: str = "ana@test.com") -> str:
    """Registra un usuario de prueba y devuelve su token de acceso."""
    client.post(
        "/api/auth/registro",
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": correo,
            "contrasena": CLAVE_VALIDA,
        },
    )
    login = client.post(
        "/api/auth/login",
        json={"correo_electronico": correo, "contrasena": CLAVE_VALIDA},
    ).json()
    return login["access_token"]


def _cabecera(token: str) -> dict[str, str]:
    """Arma el header Authorization con el token dado."""
    return {"Authorization": f"Bearer {token}"}


def test_perfil_sin_token_devuelve_401(cliente) -> None:
    """Los endpoints de perfil exigen autenticación."""
    client, _ = cliente

    respuesta = client.get("/api/usuarios/me")

    assert respuesta.status_code == 401


def test_perfil_incluye_granja_historico_y_preferencias(cliente) -> None:
    """El perfil trae granja, histórico acumulado y preferencias de alertas."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.get("/api/usuarios/me", headers=_cabecera(token))

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert "nombre_granja" in cuerpo
    assert cuerpo["total_huevos_producidos"] == 0
    assert cuerpo["total_aves_gestionadas"] == 0
    assert cuerpo["notif_produccion_baja"] is True
    assert cuerpo["notif_stock_bajo"] is True
    assert cuerpo["notif_vacunacion"] is False
    assert cuerpo["notif_resumen_semanal"] is True


def test_perfil_historico_suma_camadas_y_produccion(cliente) -> None:
    """El histórico suma las aves iniciales y todos los huevos recolectados."""
    client, db_path = cliente
    token = _registrar_y_token(client)

    motor = create_engine(f"sqlite:///{db_path}")
    sesion_fabrica = sessionmaker(bind=motor)
    with sesion_fabrica() as sesion:
        id_usuario = sesion.execute(
            text(
                "SELECT id_usuario FROM usuario "
                "WHERE correo_electronico = 'ana@test.com'"
            )
        ).scalar()
        sesion.execute(
            text(
                "INSERT INTO camada "
                "(id_usuario, nombre_camada, fecha_ingreso, cantidad_inicial, "
                " cantidad_actual, estado, edad_semanas) "
                "VALUES (:uid, 'Lote 1', :fecha, 100, 90, 'activa', 30)"
            ),
            {"uid": id_usuario, "fecha": date.today().isoformat()},
        )
        fila = sesion.execute(
            text("SELECT id_camada FROM camada WHERE id_usuario = :uid"),
            {"uid": id_usuario},
        ).scalar()
        sesion.execute(
            text(
                "INSERT INTO produccion_diaria "
                "(id_usuario, id_camada, fecha_recoleccion, total_huevos) "
                "VALUES (:uid, :cid, :fecha, 250)"
            ),
            {"uid": id_usuario, "cid": fila, "fecha": date.today().isoformat()},
        )
        sesion.commit()
    motor.dispose()

    cuerpo = client.get("/api/usuarios/me", headers=_cabecera(token)).json()

    assert cuerpo["total_aves_gestionadas"] == 100
    assert cuerpo["total_huevos_producidos"] == 250
    assert cuerpo["aves_actuales"] == 90


def test_actualizar_perfil_cambia_datos_y_granja(cliente) -> None:
    """Actualizar nombre, teléfono y granja responde 200 con los cambios."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me",
        headers=_cabecera(token),
        json={
            "nombre_completo": "Ana Actualizada",
            "correo_electronico": "ana@test.com",
            "telefono": "3001234567",
            "nombre_granja": "Granja La Esperanza",
        },
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["nombre_completo"] == "Ana Actualizada"
    assert cuerpo["telefono"] == "3001234567"
    assert cuerpo["nombre_granja"] == "Granja La Esperanza"


def test_cambiar_correo_sin_contrasena_devuelve_401(cliente) -> None:
    """Cambiar el correo sin enviar la contraseña actual responde 401."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me",
        headers=_cabecera(token),
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "nuevo@test.com",
        },
    )

    assert respuesta.status_code == 401


def test_cambiar_correo_con_contrasena_incorrecta_devuelve_401(cliente) -> None:
    """Cambiar el correo con una contraseña equivocada responde 401."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me",
        headers=_cabecera(token),
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "nuevo@test.com",
            "contrasena_actual": "Incorrecta123!",
        },
    )

    assert respuesta.status_code == 401


def test_cambiar_correo_con_contrasena_correcta(cliente) -> None:
    """Cambiar el correo con la contraseña actual permite iniciar con el nuevo."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me",
        headers=_cabecera(token),
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "nuevo@test.com",
            "contrasena_actual": CLAVE_VALIDA,
        },
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["correo_electronico"] == "nuevo@test.com"
    assert (
        client.post(
            "/api/auth/login",
            json={"correo_electronico": "nuevo@test.com", "contrasena": CLAVE_VALIDA},
        ).status_code
        == 200
    )
    assert (
        client.post(
            "/api/auth/login",
            json={"correo_electronico": "ana@test.com", "contrasena": CLAVE_VALIDA},
        ).status_code
        == 401
    )


def test_cambiar_correo_duplicado_devuelve_409(cliente) -> None:
    """Cambiar el correo a uno ya registrado responde 409."""
    client, _ = cliente
    token = _registrar_y_token(client, "ana@test.com")
    _registrar_y_token(client, "otro@test.com")

    respuesta = client.put(
        "/api/usuarios/me",
        headers=_cabecera(token),
        json={
            "nombre_completo": "Ana Test",
            "correo_electronico": "otro@test.com",
            "contrasena_actual": CLAVE_VALIDA,
        },
    )

    assert respuesta.status_code == 409


def test_actualizar_notificaciones_persiste(cliente) -> None:
    """Las preferencias de alertas se guardan y se leen de vuelta."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me/notificaciones",
        headers=_cabecera(token),
        json={
            "notif_produccion_baja": False,
            "notif_stock_bajo": True,
            "notif_vacunacion": True,
            "notif_resumen_semanal": False,
        },
    )

    assert respuesta.status_code == 200
    perfil = client.get("/api/usuarios/me", headers=_cabecera(token)).json()
    assert perfil["notif_produccion_baja"] is False
    assert perfil["notif_stock_bajo"] is True
    assert perfil["notif_vacunacion"] is True
    assert perfil["notif_resumen_semanal"] is False


def test_cambiar_contrasena_con_actual_incorrecta_devuelve_401(cliente) -> None:
    """Cambiar la contraseña con la actual equivocada responde 401."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me/contrasena",
        headers=_cabecera(token),
        json={
            "contrasena_actual": "Incorrecta123!",
            "contrasena_nueva": "Nueva1234!",
        },
    )

    assert respuesta.status_code == 401


def test_cambiar_contrasena_exitoso(cliente) -> None:
    """Con la contraseña actual correcta, la nueva queda activa."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me/contrasena",
        headers=_cabecera(token),
        json={
            "contrasena_actual": CLAVE_VALIDA,
            "contrasena_nueva": "Nueva1234!",
        },
    )

    assert respuesta.status_code == 200
    assert (
        client.post(
            "/api/auth/login",
            json={"correo_electronico": "ana@test.com", "contrasena": CLAVE_VALIDA},
        ).status_code
        == 401
    )
    assert (
        client.post(
            "/api/auth/login",
            json={"correo_electronico": "ana@test.com", "contrasena": "Nueva1234!"},
        ).status_code
        == 200
    )


def test_cambiar_contrasena_debil_devuelve_422(cliente) -> None:
    """Una contraseña nueva que no cumple la política responde 422."""
    client, _ = cliente
    token = _registrar_y_token(client)

    respuesta = client.put(
        "/api/usuarios/me/contrasena",
        headers=_cabecera(token),
        json={
            "contrasena_actual": CLAVE_VALIDA,
            "contrasena_nueva": "debil",
        },
    )

    assert respuesta.status_code == 422

"""Pruebas del aviso diario de producción baja.

El aviso se genera de forma perezosa al listar las notificaciones: revisa
la recolección de ayer de cada camada en producción y avisa cuando la
postura quedó por debajo del 85% de las aves activas.

La API solo permite registrar producción del día actual, así que la
recolección de ayer se inserta directo en la base de prueba.
"""

from datetime import date, datetime, time, timedelta
from pathlib import Path

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
EDAD_INICIAL = 16
EDAD_PRODUCCION = 28
AYER = date.today() - timedelta(days=1)


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
    return {"Authorization": f"Bearer {crear_token_acceso(
        sub=str(respuesta.json()['id_usuario'])
    )}"}


def _configurar_hora(cliente, headers, hora: time) -> None:
    """Cambia la hora del análisis diario en las preferencias."""
    respuesta = cliente.put(
        "/api/usuarios/me/notificaciones",
        headers=headers,
        json={
            "notif_produccion_baja": True,
            "notif_stock_bajo": True,
            "notif_vacunacion": False,
            "notif_resumen_semanal": True,
            "notif_produccion_hora": hora.strftime("%H:%M"),
        },
    )
    assert respuesta.status_code == 200


def _camada_en_produccion(
    cliente, headers, nombre: str = "Camada Prueba", aves: int = 100
) -> dict:
    """Crea una camada de ayer y la lleva a la etapa de producción."""
    creacion = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": nombre,
            "fecha_ingreso": (date.today() - timedelta(days=1)).isoformat(),
            "cantidad_inicial": aves,
        },
        headers=headers,
    )
    assert creacion.status_code == 201
    id_camada = creacion.json()["id_camada"]
    respuesta = creacion
    for _ in range(EDAD_PRODUCCION - EDAD_INICIAL):
        respuesta = cliente.post(
            f"/api/camadas/{id_camada}/avanzar-semana", headers=headers
        )
        assert respuesta.status_code == 200
    return respuesta.json()


def _registrar_produccion_ayer(db_path: Path, id_camada: int, huevos: int) -> None:
    """Inserta la recolección de ayer para la camada indicada."""
    engine = create_engine(f"sqlite:///{db_path}")
    with engine.begin() as conn:
        id_usuario = conn.execute(
            text("SELECT id_usuario FROM camada WHERE id_camada = :id"),
            {"id": id_camada},
        ).scalar_one()
        conn.execute(
            text(
                "INSERT INTO produccion_diaria "
                "(id_usuario, id_camada, fecha_recoleccion, total_huevos) "
                "VALUES (:usuario, :camada, :fecha, :huevos)"
            ),
            {
                "usuario": id_usuario,
                "camada": id_camada,
                "fecha": AYER.isoformat(),
                "huevos": huevos,
            },
        )
    engine.dispose()


def _avisos_produccion(cliente, headers) -> list:
    """Devuelve solo los avisos de producción del usuario."""
    avisos = cliente.get("/api/notificaciones", headers=headers).json()
    return [
        aviso
        for aviso in avisos
        if aviso["tipo"] in ("produccion_baja", "produccion_sin_registro")
    ]


def test_produccion_bajo_el_85_genera_aviso(cliente) -> None:
    """80 huevos para 100 aves (80%) genera el aviso de producción baja."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod1@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers, "Camada La Esperanza")
    _registrar_produccion_ayer(db_path, camada["id_camada"], 80)

    avisos = _avisos_produccion(client, headers)

    assert len(avisos) == 1
    aviso = avisos[0]
    assert aviso["tipo"] == "produccion_baja"
    assert aviso["titulo"] == "Producción baja"
    assert aviso["id_camada"] == camada["id_camada"]
    assert "80 huevos" in aviso["mensaje"]
    assert "80%" in aviso["mensaje"]


def test_produccion_justo_en_el_85_no_genera_aviso(cliente) -> None:
    """85 huevos para 100 aves es el límite: no se avisa."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod2@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 85)

    assert _avisos_produccion(client, headers) == []


def test_produccion_normal_no_genera_aviso(cliente) -> None:
    """Una postura por encima del umbral se considera normal."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod3@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 96)

    assert _avisos_produccion(client, headers) == []


def test_sin_registro_genera_aviso_distinto(cliente) -> None:
    """Si no hay recolección de ayer, el aviso pide registrarla."""
    client, _ = cliente
    headers = _registrar_usuario(client, "prod4@test.com")
    _configurar_hora(client, headers, time(0, 0))
    _camada_en_produccion(client, headers, "Camada Sin Registro")

    avisos = _avisos_produccion(client, headers)

    assert len(avisos) == 1
    assert avisos[0]["tipo"] == "produccion_sin_registro"
    assert avisos[0]["titulo"] == "Falta el registro de producción"
    assert "No registraste la recolección" in avisos[0]["mensaje"]


def test_camada_fuera_de_produccion_no_avisa(cliente) -> None:
    """Una camada de 20 semanas aún no produce huevos: no se evalúa."""
    client, _ = cliente
    headers = _registrar_usuario(client, "prod5@test.com")
    _configurar_hora(client, headers, time(0, 0))
    creacion = client.post(
        "/api/camadas",
        json={
            "nombre_camada": "Camada Joven",
            "fecha_ingreso": (date.today() - timedelta(days=1)).isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    id_camada = creacion.json()["id_camada"]
    for _ in range(EDAD_PRODUCCION - EDAD_INICIAL - 4):
        client.post(f"/api/camadas/{id_camada}/avanzar-semana", headers=headers)

    assert _avisos_produccion(client, headers) == []


def test_preferencia_apagada_no_genera_aviso(cliente) -> None:
    """Si el usuario apagó la alerta de producción, no se avisa."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod6@test.com")
    respuesta = client.put(
        "/api/usuarios/me/notificaciones",
        headers=headers,
        json={
            "notif_produccion_baja": False,
            "notif_stock_bajo": True,
            "notif_vacunacion": False,
            "notif_resumen_semanal": True,
            "notif_produccion_hora": "00:00",
        },
    )
    assert respuesta.status_code == 200
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 50)

    assert _avisos_produccion(client, headers) == []


def test_no_avisa_antes_de_la_hora(cliente) -> None:
    """Antes de la hora configurada todavía no se evalúa la producción."""
    client, db_path = cliente
    ahora = datetime.now()
    if ahora.time() >= time(23, 30):
        pytest.skip("La prueba no es fiable a menos de 30 minutos de medianoche")
    headers = _registrar_usuario(client, "prod7@test.com")
    _configurar_hora(
        client, headers, (ahora + timedelta(minutes=30)).time().replace(second=0)
    )
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 50)

    assert _avisos_produccion(client, headers) == []


def test_no_duplica_el_aviso_del_dia(cliente) -> None:
    """Listar varias veces no repite el aviso de la misma fecha."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod8@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 60)

    client.get("/api/notificaciones", headers=headers)
    segunda = _avisos_produccion(client, headers)

    assert len(segunda) == 1


def test_mortalidad_de_hoy_ajusta_las_aves_de_ayer(cliente) -> None:
    """Ayer había 100 aves: 80 huevos es 80%, aunque hoy queden 90."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod9@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers, aves=100)
    registro = client.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 10},
        headers=headers,
    )
    assert registro.status_code == 200
    _registrar_produccion_ayer(db_path, camada["id_camada"], 80)

    avisos = _avisos_produccion(client, headers)

    assert len(avisos) == 1
    assert "100 aves" in avisos[0]["mensaje"]


def test_aviso_eliminado_no_se_regenera(cliente) -> None:
    """Si el usuario borra el aviso del día, no vuelve a aparecer."""
    client, db_path = cliente
    headers = _registrar_usuario(client, "prod10@test.com")
    _configurar_hora(client, headers, time(0, 0))
    camada = _camada_en_produccion(client, headers)
    _registrar_produccion_ayer(db_path, camada["id_camada"], 40)
    aviso = _avisos_produccion(client, headers)[0]

    borrar = client.delete(
        f"/api/notificaciones/{aviso['id_notificacion']}", headers=headers
    )

    assert borrar.status_code == 204
    assert _avisos_produccion(client, headers) == []

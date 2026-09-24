from datetime import date, datetime, timedelta
from pathlib import Path

import pytest
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from app.models.camada import Camada
from app.models.evento_sanitario import EventoSanitario
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
EDAD_INICIAL = 16
EDAD_DECISION = 72


@pytest.fixture()
def cliente(tmp_path: Path):
    """Levanta la API con una base SQLite temporal cargada con el seed.

    Returns:
        TestClient: Cliente de prueba con la sesión de datos adjunta.
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
        "fecha_ingreso": date.today().isoformat(),
        "cantidad_inicial": 100,
    }
    datos.update(reemplazos)
    return datos


def _crear_camada(cliente, headers, **reemplazos) -> dict:
    """Crea una camada y devuelve el JSON de la respuesta."""
    respuesta = cliente.post(
        "/api/camadas", json=_datos_camada(**reemplazos), headers=headers
    )
    assert respuesta.status_code == 201
    return respuesta.json()


def _avanzar_hasta(cliente, headers, id_camada: int, objetivo: int) -> dict:
    """Avanza una camada semana a semana hasta la edad objetivo."""
    for _ in range(objetivo - EDAD_INICIAL):
        respuesta = cliente.post(
            f"/api/camadas/{id_camada}/avanzar-semana", headers=headers
        )
        assert respuesta.status_code == 200
    return respuesta.json()


def test_crear_camada_responde_201_con_edad_inicial(cliente) -> None:
    """Crear una camada responde 201, arranca en 16 semanas y activa."""
    headers = _registrar_usuario(cliente, "camada1@test.com")

    respuesta = cliente.post("/api/camadas", json=_datos_camada(), headers=headers)

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["cantidad_actual"] == cuerpo["cantidad_inicial"] == 100
    assert cuerpo["estado"] == "activa"
    assert cuerpo["edad_semanas"] == EDAD_INICIAL
    assert cuerpo["puede_editar_inicial"] is True


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


def test_crear_camada_con_fecha_de_ayer(cliente) -> None:
    """Se puede registrar una camada con la fecha de ayer."""
    headers = _registrar_usuario(cliente, "camadafecha1@test.com")
    ayer = (date.today() - timedelta(days=1)).isoformat()

    respuesta = cliente.post(
        "/api/camadas",
        json=_datos_camada(fecha_ingreso=ayer),
        headers=headers,
    )

    assert respuesta.status_code == 201
    assert respuesta.json()["fecha_ingreso"] == ayer


def test_crear_camada_con_fecha_antigua_devuelve_400(cliente) -> None:
    """Una fecha anterior a ayer no se admite."""
    headers = _registrar_usuario(cliente, "camadafecha2@test.com")
    anteayer = (date.today() - timedelta(days=2)).isoformat()

    respuesta = cliente.post(
        "/api/camadas",
        json=_datos_camada(fecha_ingreso=anteayer),
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_crear_camada_con_fecha_futura_devuelve_400(cliente) -> None:
    """Una fecha futura no se admite."""
    headers = _registrar_usuario(cliente, "camadafecha3@test.com")
    manana = (date.today() + timedelta(days=1)).isoformat()

    respuesta = cliente.post(
        "/api/camadas",
        json=_datos_camada(fecha_ingreso=manana),
        headers=headers,
    )

    assert respuesta.status_code == 400


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
    activa = _crear_camada(cliente, headers, nombre_camada="Activa Uno")
    retirada = _crear_camada(
        cliente, headers, nombre_camada="Retirada Uno", estado="retirada"
    )

    solo_activas = cliente.get("/api/camadas?estado=activa", headers=headers).json()
    solo_retiradas = cliente.get("/api/camadas?estado=retirada", headers=headers).json()
    todas = cliente.get("/api/camadas", headers=headers).json()

    assert [c["id_camada"] for c in solo_activas] == [activa["id_camada"]]
    assert [c["id_camada"] for c in solo_retiradas] == [retirada["id_camada"]]
    assert len(todas) == 2


def test_obtener_camada_de_otro_usuario_devuelve_404(cliente) -> None:
    """Una camada ajena no es visible para otro usuario (404)."""
    dueno = _registrar_usuario(cliente, "camada5@test.com")
    camada = _crear_camada(cliente, dueno)
    intruso = _registrar_usuario(cliente, "camada6@test.com")

    respuesta = cliente.get(f"/api/camadas/{camada['id_camada']}", headers=intruso)

    assert respuesta.status_code == 404


def test_actualizar_camada_nombre(cliente) -> None:
    """PATCH con nombre válido actualiza solo el nombre."""
    headers = _registrar_usuario(cliente, "camada7@test.com")
    camada = _crear_camada(cliente, headers, nombre_camada="Original")

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"nombre_camada": "Renombrada"},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["nombre_camada"] == "Renombrada"


def test_actualizar_camada_fecha_ingreso_prohibida(cliente) -> None:
    """La fecha de ingreso ya no es editable (422)."""
    headers = _registrar_usuario(cliente, "camada7b@test.com")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"fecha_ingreso": "2026-02-01"},
        headers=headers,
    )

    assert respuesta.status_code == 422


def _envejecer_camada(cliente, id_camada: int, horas: int) -> None:
    """Mueve la fecha de creación de una camada hacia atrás para probar."""
    sesion = cliente.session_prueba()
    try:
        camada = sesion.get(Camada, id_camada)
        camada.fecha_creacion = datetime.now() - timedelta(hours=horas)
        sesion.commit()
    finally:
        sesion.close()


def test_editar_cantidad_inicial_suma_la_diferencia(cliente) -> None:
    """Dentro de 24h, subir la inicial suma la diferencia a la actual."""
    headers = _registrar_usuario(cliente, "camada8@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=12)
    cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 2},
        headers=headers,
    )

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_inicial": 20},
        headers=headers,
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["cantidad_inicial"] == 20
    assert cuerpo["cantidad_actual"] == 18


def test_editar_cantidad_inicial_resta_la_diferencia(cliente) -> None:
    """Dentro de 24h, bajar la inicial resta la diferencia a la actual."""
    headers = _registrar_usuario(cliente, "camada8b@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=12)
    cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 2},
        headers=headers,
    )

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_inicial": 10},
        headers=headers,
    )

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["cantidad_inicial"] == 10
    assert cuerpo["cantidad_actual"] == 8


def test_editar_cantidad_inicial_ajuste_negativo_400(cliente) -> None:
    """El ajuste no puede dejar la cantidad actual en negativo."""
    headers = _registrar_usuario(cliente, "camada8c@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=12)
    cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 11},
        headers=headers,
    )

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_inicial": 10},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_editar_cantidad_inicial_fuera_de_24h_400(cliente) -> None:
    """Pasadas las 24 horas no se puede editar la cantidad inicial."""
    headers = _registrar_usuario(cliente, "camada8d@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=12)
    _envejecer_camada(cliente, camada["id_camada"], 25)

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_inicial": 20},
        headers=headers,
    )

    assert respuesta.status_code == 400
    detalle = cliente.get(f"/api/camadas/{camada['id_camada']}", headers=headers).json()
    assert detalle["puede_editar_inicial"] is False


def test_editar_nombre_fuera_de_24h_permitido(cliente) -> None:
    """El nombre se puede editar aunque pasen las 24 horas."""
    headers = _registrar_usuario(cliente, "camada8e@test.com")
    camada = _crear_camada(cliente, headers, nombre_camada="Original")
    _envejecer_camada(cliente, camada["id_camada"], 25)

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"nombre_camada": "Renombrada tarde"},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["nombre_camada"] == "Renombrada tarde"


def test_editar_cantidad_inicial_excede_plan_400(cliente) -> None:
    """Aumentar la cantidad inicial no puede superar el límite del plan."""
    headers = _registrar_usuario(cliente, "camada8f@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=300)

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_inicial": 400},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_actualizar_camada_con_campo_prohibido_devuelve_422(cliente) -> None:
    """No se puede editar cantidad_actual ni estado por PATCH."""
    headers = _registrar_usuario(cliente, "camada9@test.com")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"cantidad_actual": 50},
        headers=headers,
    )

    assert respuesta.status_code == 422


def test_actualizar_camada_retirada_devuelve_400(cliente) -> None:
    """Una camada retirada no se puede editar."""
    headers = _registrar_usuario(cliente, "camada10@test.com")
    camada = _crear_camada(cliente, headers, estado="retirada")

    respuesta = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"nombre_camada": "No editable"},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_mortalidad_valida_descuenta_y_crea_evento(cliente) -> None:
    """Registrar mortalidad descuenta aves y deja un evento sanitario."""
    headers = _registrar_usuario(cliente, "camada11@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=100)

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 10},
        headers=headers,
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["cantidad_actual"] == 90

    sesion = cliente.session_prueba()
    try:
        eventos = (
            sesion.query(EventoSanitario)
            .filter(EventoSanitario.id_camada == camada["id_camada"])
            .all()
        )
    finally:
        sesion.close()
    assert len(eventos) == 1
    assert eventos[0].tipo_evento == "mortalidad"
    assert eventos[0].mortalidad == 10


def test_mortalidad_que_excede_cantidad_actual_devuelve_400(cliente) -> None:
    """Una mortalidad mayor a la cantidad actual responde 400."""
    headers = _registrar_usuario(cliente, "camada12@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=100)

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 101},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_mortalidad_cero_o_no_entera_devuelve_422(cliente) -> None:
    """La mortalidad debe ser un entero mayor que cero."""
    headers = _registrar_usuario(cliente, "camada13@test.com")
    camada = _crear_camada(cliente, headers, cantidad_inicial=100)

    cero = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 0},
        headers=headers,
    )
    decimal = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 1.5},
        headers=headers,
    )

    assert cero.status_code == 422
    assert decimal.status_code == 422


def test_mortalidad_en_camada_retirada_devuelve_400(cliente) -> None:
    """No se registra mortalidad en una camada retirada."""
    headers = _registrar_usuario(cliente, "camada14@test.com")
    camada = _crear_camada(cliente, headers, estado="retirada")

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/mortalidad",
        json={"cantidad": 1},
        headers=headers,
    )

    assert respuesta.status_code == 400


def test_edad_y_retiro_estimado(cliente) -> None:
    """La edad en días y el retiro estimado parten de las 16 semanas."""
    headers = _registrar_usuario(cliente, "camada15@test.com")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.get(f"/api/camadas/{camada['id_camada']}/edad", headers=headers)

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["edad_dias"] == EDAD_INICIAL * 7
    esperado = (date.today() + timedelta(days=504 - EDAD_INICIAL * 7)).isoformat()
    assert cuerpo["fecha_retiro_estimada"] == esperado


def test_avanzar_semana_suma_una_semana(cliente) -> None:
    """POST avanzar-semana suma 7 días de vida."""
    headers = _registrar_usuario(cliente, "camada16@test.com")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/avanzar-semana", headers=headers
    )

    assert respuesta.status_code == 200
    assert respuesta.json()["edad_semanas"] == EDAD_INICIAL + 1


def test_avanzar_semana_en_retirada_devuelve_400(cliente) -> None:
    """No se avanza semana en una camada retirada."""
    headers = _registrar_usuario(cliente, "camada17@test.com")
    camada = _crear_camada(cliente, headers, estado="retirada")

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/avanzar-semana", headers=headers
    )

    assert respuesta.status_code == 400


def test_camada_no_se_retira_sola_al_llegar_a_72_semanas(cliente) -> None:
    """Al superar la vida productiva la camada sigue activa."""
    headers = _registrar_usuario(cliente, "camada18@test.com")
    camada = _crear_camada(cliente, headers)

    cuerpo = _avanzar_hasta(cliente, headers, camada["id_camada"], EDAD_DECISION)

    assert cuerpo["edad_semanas"] == EDAD_DECISION
    assert cuerpo["estado"] == "activa"
    assert cuerpo["requiere_decision"] is True


def test_alertas_y_seguir_activa_pospone_aviso(cliente) -> None:
    """La alerta aparece a las 72 semanas y seguir-activa la pospone 7 días."""
    headers = _registrar_usuario(cliente, "camada19@test.com")
    camada = _crear_camada(cliente, headers)
    _avanzar_hasta(cliente, headers, camada["id_camada"], EDAD_DECISION)

    alertas = cliente.get("/api/camadas/alertas", headers=headers).json()
    assert [c["id_camada"] for c in alertas] == [camada["id_camada"]]

    seguir = cliente.post(
        f"/api/camadas/{camada['id_camada']}/seguir-activa", headers=headers
    )
    assert seguir.status_code == 200
    esperado = (date.today() + timedelta(days=7)).isoformat()
    assert seguir.json()["fecha_proximo_aviso"] == esperado
    assert seguir.json()["requiere_decision"] is False

    alertas = cliente.get("/api/camadas/alertas", headers=headers).json()
    assert alertas == []


def test_seguir_activa_sin_decision_devuelve_400(cliente) -> None:
    """Seguir activa exige que la camada pida decisión."""
    headers = _registrar_usuario(cliente, "camada20@test.com")
    camada = _crear_camada(cliente, headers)

    respuesta = cliente.post(
        f"/api/camadas/{camada['id_camada']}/seguir-activa", headers=headers
    )

    assert respuesta.status_code == 400


def test_descartar_camada_y_no_reactivar(cliente) -> None:
    """Descartar deja la camada retirada y no se puede reactivar."""
    headers = _registrar_usuario(cliente, "camada21@test.com")
    camada = _crear_camada(cliente, headers)

    descartar = cliente.post(
        f"/api/camadas/{camada['id_camada']}/descartar", headers=headers
    )
    assert descartar.status_code == 200
    assert descartar.json()["estado"] == "retirada"

    repetir = cliente.post(
        f"/api/camadas/{camada['id_camada']}/descartar", headers=headers
    )
    assert repetir.status_code == 400

    reactivar = cliente.patch(
        f"/api/camadas/{camada['id_camada']}",
        json={"estado": "activa"},
        headers=headers,
    )
    assert reactivar.status_code == 422

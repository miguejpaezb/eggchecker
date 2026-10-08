"""Pruebas del módulo de análisis de huevos con IA (RF-33 a RF-36, CU-06).

Ninguna prueba llama a internet: el proveedor de IA se reemplaza por uno
falso y el cliente de Gemini se prueba con un transporte simulado.
"""

import json
from datetime import date
from pathlib import Path

import httpx
import pytest
from app.api.analisis_ia import get_proveedor_ia
from app.core.config import Settings, get_settings
from app.core.database import get_db
from app.core.security import crear_token_acceso
from app.main import create_app
from app.models.usuario import Usuario
from app.schemas.analisis_ia import DiagnosticoIA
from app.services.ia_proveedor import (
    DemoProveedor,
    GeminiProveedor,
    ProveedorIAError,
    extraer_json,
    normalizar_diagnostico,
)
from app.services.recomendaciones_ia import ContextoGranja, generar_recomendaciones
from fastapi.testclient import TestClient
from scripts.cargar_seed import cargar_seed
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

CLAVE_VALIDA = "Test1234!"
# Encabezado mínimo de un JPEG: basta para la validación por firma.
JPEG = b"\xff\xd8\xff\xe0" + b"\x00" * 64
PNG = b"\x89PNG\r\n\x1a\n" + b"\x00" * 64
LIMITE_DIARIO = 3


class ProveedorFalso:
    """Proveedor configurable para las pruebas de la API."""

    nombre = "falso"
    es_demo = False

    def __init__(self) -> None:
        self.diagnostico = DemoProveedor().diagnosticar(b"", "image/jpeg")
        self.diagnostico = self.diagnostico.model_copy(
            update={"resumen": "Lote con suciedad leve y una fisura."}
        )
        self.error: str | None = None
        self.llamadas = 0

    def diagnosticar(self, imagen: bytes, mime_type: str) -> DiagnosticoIA:
        self.llamadas += 1
        if self.error:
            raise ProveedorIAError(self.error)
        return self.diagnostico


@pytest.fixture()
def proveedor() -> ProveedorFalso:
    return ProveedorFalso()


@pytest.fixture()
def cliente(tmp_path: Path, proveedor: ProveedorFalso):
    """API con base SQLite temporal, carpeta de fotos temporal e IA falsa."""
    db_path = tmp_path / "test.db"
    cargar_seed(db_path)
    engine = create_engine(
        f"sqlite:///{db_path}", connect_args={"check_same_thread": False}
    )
    session_prueba = sessionmaker(bind=engine, autocommit=False, autoflush=False)
    settings = Settings(
        IA_UPLOAD_DIR=str(tmp_path / "uploads"),
        IA_LIMITE_DIARIO=LIMITE_DIARIO,
        IA_MAX_MB=1,
    )

    def override_get_db():
        db = session_prueba()
        try:
            yield db
        finally:
            db.close()

    app = create_app()
    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_settings] = lambda: settings
    app.dependency_overrides[get_proveedor_ia] = lambda: proveedor
    client = TestClient(app)
    client.session_prueba = session_prueba
    client.carpeta = tmp_path / "uploads"
    yield client
    client.close()
    engine.dispose()


def _registrar(cliente, correo: str, premium: bool = True) -> dict:
    """Registra un usuario (opcionalmente Premium) y devuelve su header."""
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
    if premium:
        with cliente.session_prueba() as db:
            db.get(Usuario, id_usuario).plan_suscripcion = "premium"
            db.commit()
    return {"Authorization": f"Bearer {crear_token_acceso(sub=str(id_usuario))}"}


def _crear_camada(cliente, headers: dict, nombre: str = "Lote Norte") -> int:
    respuesta = cliente.post(
        "/api/camadas",
        json={
            "nombre_camada": nombre,
            "fecha_ingreso": date.today().isoformat(),
            "cantidad_inicial": 100,
        },
        headers=headers,
    )
    assert respuesta.status_code == 201
    return respuesta.json()["id_camada"]


def _analizar(cliente, headers, contenido=JPEG, id_camada=None):
    datos = {"id_camada": str(id_camada)} if id_camada is not None else {}
    return cliente.post(
        "/api/analisis-ia",
        files={"imagen": ("huevos.jpg", contenido, "image/jpeg")},
        data=datos,
        headers=headers,
    )


# ── Acceso por plan (CU-06: Usuario Premium) ────────────────────────────


def test_usuario_gratuito_no_puede_analizar(cliente):
    headers = _registrar(cliente, "gratis@test.com", premium=False)

    estado = cliente.get("/api/analisis-ia/estado", headers=headers)
    assert estado.status_code == 200
    assert estado.json()["disponible"] is False

    respuesta = _analizar(cliente, headers)
    assert respuesta.status_code == 403
    assert "Premium" in respuesta.json()["detail"]
    assert cliente.get("/api/analisis-ia", headers=headers).status_code == 403


def test_sin_token_responde_401(cliente):
    assert cliente.get("/api/analisis-ia/estado").status_code == 401


def test_estado_premium_informa_cupo(cliente):
    headers = _registrar(cliente, "estado@test.com")
    datos = cliente.get("/api/analisis-ia/estado", headers=headers).json()
    assert datos["disponible"] is True
    assert datos["limite_diario"] == LIMITE_DIARIO
    assert datos["usados_hoy"] == 0
    assert datos["modo_demo"] is False


# ── RF-33 a RF-36: flujo completo ────────────────────────────────────────


def test_analisis_completo_con_camada(cliente):
    headers = _registrar(cliente, "premium@test.com")
    id_camada = _crear_camada(cliente, headers)

    respuesta = _analizar(cliente, headers, id_camada=id_camada)

    assert respuesta.status_code == 201
    datos = respuesta.json()
    assert datos["id_camada"] == id_camada
    assert datos["nombre_camada"] == "Lote Norte"
    assert datos["huevos_detectados"] == 12
    assert datos["calidad_general"] == "regular"
    assert datos["tiene_imagen"] is True
    assert datos["resultado_diagnostico"].startswith("Muestra 12 huevos: 50% AA")
    assert {a["tipo"] for a in datos["anomalias"]} == {"suciedad", "grieta"}
    assert datos["distribucion"] == {"AA": 6, "A": 3, "B": 2, "No_apto": 1}
    # RF-35: las recomendaciones mencionan la camada y cierran con el aviso.
    recs = datos["recomendaciones"]
    assert any("Lote Norte" in r for r in recs)
    assert any("calcio" in r for r in recs)
    assert "veterinario" in recs[-1]

    # La foto se guarda y solo su dueño la descarga.
    imagen = cliente.get(
        f"/api/analisis-ia/{datos['id_analisis']}/imagen", headers=headers
    )
    assert imagen.status_code == 200
    assert imagen.content == JPEG


def test_historial_por_camada(cliente):
    headers = _registrar(cliente, "historial@test.com")
    camada_a = _crear_camada(cliente, headers, "Lote A")
    camada_b = _crear_camada(cliente, headers, "Lote B")
    _analizar(cliente, headers, id_camada=camada_a)
    _analizar(cliente, headers, id_camada=camada_b)
    _analizar(cliente, headers)

    todos = cliente.get("/api/analisis-ia", headers=headers).json()
    solo_a = cliente.get(
        "/api/analisis-ia", params={"id_camada": camada_a}, headers=headers
    ).json()

    assert len(todos) == 3
    assert len(solo_a) == 1
    assert solo_a[0]["nombre_camada"] == "Lote A"


def test_historial_del_seed_sin_detalle(cliente):
    headers = {"Authorization": f"Bearer {crear_token_acceso(sub='1')}"}
    lista = cliente.get("/api/analisis-ia", headers=headers).json()
    assert len(lista) == 2
    assert lista[0]["tiene_imagen"] is False
    assert lista[0]["anomalias"] == []
    assert lista[0]["distribucion"] is None
    assert lista[0]["recomendaciones"]


def test_png_es_aceptado(cliente):
    headers = _registrar(cliente, "png@test.com")
    assert _analizar(cliente, headers, contenido=PNG).status_code == 201


# ── Validaciones ─────────────────────────────────────────────────────────


@pytest.mark.parametrize(
    ("contenido", "codigo"),
    [
        (b"", 400),
        (b"GIF89a" + b"\x00" * 20, 415),
        (b"%PDF-1.4 no es imagen", 415),
        (b"\xff\xd8\xff" + b"\x00" * (1024 * 1024), 413),
    ],
)
def test_imagen_invalida(cliente, proveedor, contenido, codigo):
    headers = _registrar(cliente, f"invalida{codigo}{len(contenido)}@test.com")
    assert _analizar(cliente, headers, contenido=contenido).status_code == codigo
    assert proveedor.llamadas == 0


def test_camada_de_otro_usuario(cliente):
    dueno = _registrar(cliente, "dueno@test.com")
    intruso = _registrar(cliente, "intruso@test.com")
    id_camada = _crear_camada(cliente, dueno)
    assert _analizar(cliente, intruso, id_camada=id_camada).status_code == 404


def test_foto_sin_huevos_no_se_guarda(cliente, proveedor):
    headers = _registrar(cliente, "sinhuevos@test.com")
    proveedor.diagnostico = proveedor.diagnostico.model_copy(
        update={"es_imagen_de_huevos": False, "huevos_detectados": 0}
    )
    respuesta = _analizar(cliente, headers)
    assert respuesta.status_code == 422
    assert cliente.get("/api/analisis-ia", headers=headers).json() == []
    assert not cliente.carpeta.exists() or not any(cliente.carpeta.rglob("*.jpg"))


def test_error_del_proveedor_responde_503(cliente, proveedor):
    headers = _registrar(cliente, "caido@test.com")
    proveedor.error = "Se agotó el cupo gratuito de análisis por ahora."
    respuesta = _analizar(cliente, headers)
    assert respuesta.status_code == 503
    assert "cupo" in respuesta.json()["detail"]


def test_limite_diario(cliente):
    headers = _registrar(cliente, "limite@test.com")
    for _ in range(LIMITE_DIARIO):
        assert _analizar(cliente, headers).status_code == 201
    respuesta = _analizar(cliente, headers)
    assert respuesta.status_code == 429
    estado = cliente.get("/api/analisis-ia/estado", headers=headers).json()
    assert estado["usados_hoy"] == LIMITE_DIARIO


# ── Retroalimentación, privacidad y borrado ─────────────────────────────


def test_retroalimentacion_y_eliminacion(cliente):
    headers = _registrar(cliente, "feedback@test.com")
    id_analisis = _analizar(cliente, headers).json()["id_analisis"]
    archivos = list(cliente.carpeta.rglob("*.jpg"))
    assert len(archivos) == 1

    respuesta = cliente.patch(
        f"/api/analisis-ia/{id_analisis}/retroalimentacion",
        json={"diagnostico_correcto": False},
        headers=headers,
    )
    assert respuesta.status_code == 200
    assert respuesta.json()["diagnostico_correcto"] is False

    borrado = cliente.delete(f"/api/analisis-ia/{id_analisis}", headers=headers)
    assert borrado.status_code == 204
    assert not archivos[0].exists()
    assert (
        cliente.get(f"/api/analisis-ia/{id_analisis}", headers=headers).status_code
        == 404
    )


def test_otro_usuario_no_ve_el_analisis(cliente):
    dueno = _registrar(cliente, "privado@test.com")
    otro = _registrar(cliente, "curioso@test.com")
    id_analisis = _analizar(cliente, dueno).json()["id_analisis"]
    assert (
        cliente.get(f"/api/analisis-ia/{id_analisis}", headers=otro).status_code == 404
    )
    assert (
        cliente.get(f"/api/analisis-ia/{id_analisis}/imagen", headers=otro).status_code
        == 404
    )


# ── Unidad: normalización, recomendaciones y cliente de Gemini ──────────


def test_normalizar_corrige_valores_del_modelo():
    diagnostico = normalizar_diagnostico(
        {
            "es_imagen_de_huevos": True,
            "huevos_detectados": "10",
            "calidad_general": "BUENA",
            "puntaje_calidad": 140,
            "apto_venta": True,
            "resumen": "Bien",
            "anomalias": [
                {
                    "tipo": "hongo",
                    "descripcion": "x",
                    "gravedad": "alta",
                    "huevos_afectados": 1,
                    "confianza": 85,
                }
            ],
            "distribucion": {"AA": 10},
            "recomendaciones": ["  ", "Recoger seguido"],
            "confianza_general": 0.9,
        }
    )
    assert diagnostico.huevos_detectados == 10
    assert diagnostico.calidad_general == "buena"
    assert diagnostico.puntaje_calidad == 100
    assert diagnostico.anomalias[0].tipo == "otro"
    assert diagnostico.anomalias[0].gravedad == "moderada"
    assert diagnostico.anomalias[0].confianza == pytest.approx(0.85)
    assert diagnostico.distribucion.B == 0
    assert diagnostico.recomendaciones == ["Recoger seguido"]


def test_extraer_json_con_bloque_de_codigo():
    assert extraer_json('```json\n{"a": 1}\n```') == {"a": 1}
    with pytest.raises(ProveedorIAError):
        extraer_json("no hay json")


def test_recomendaciones_por_edad_de_la_camada():
    diagnostico = DemoProveedor().diagnosticar(b"", "image/jpeg")
    vieja = generar_recomendaciones(
        diagnostico, ContextoGranja(nombre_camada="Lote 1", edad_semanas=70)
    )
    assert any("70 semanas" in r for r in vieja)
    assert any("Separa 1 huevo No apto" in r for r in vieja)
    assert len(vieja) <= 6


def test_recomendaciones_postura_baja():
    diagnostico = DemoProveedor().diagnosticar(b"", "image/jpeg")
    recs = generar_recomendaciones(
        diagnostico, ContextoGranja(nombre_camada="Lote 2", postura_promedio=0.42)
    )
    assert any("42%" in r for r in recs)


def _respuesta_gemini(datos: dict) -> dict:
    return {"candidates": [{"content": {"parts": [{"text": json.dumps(datos)}]}}]}


def test_gemini_usa_modelo_de_respaldo_si_se_agota_el_cupo():
    peticiones: list[httpx.Request] = []
    diagnostico = DemoProveedor().diagnosticar(b"", "image/jpeg").model_dump()

    def manejador(peticion: httpx.Request) -> httpx.Response:
        peticiones.append(peticion)
        if "modelo-principal" in peticion.url.path:
            return httpx.Response(429, json={"error": {"code": 429}})
        return httpx.Response(200, json=_respuesta_gemini(diagnostico))

    settings = Settings(
        GEMINI_API_KEY="llave-de-prueba",
        GEMINI_MODEL="modelo-principal",
        GEMINI_MODEL_RESPALDO="modelo-respaldo",
    )
    proveedor = GeminiProveedor(
        settings, cliente=httpx.Client(transport=httpx.MockTransport(manejador))
    )

    resultado = proveedor.diagnosticar(JPEG, "image/jpeg")

    assert resultado.huevos_detectados == 12
    assert proveedor.nombre == "gemini:modelo-respaldo"
    assert len(peticiones) == 2
    cuerpo = json.loads(peticiones[0].content)
    assert peticiones[0].headers["x-goog-api-key"] == "llave-de-prueba"
    assert cuerpo["contents"][0]["parts"][0]["inline_data"]["mime_type"] == (
        "image/jpeg"
    )
    assert cuerpo["generationConfig"]["responseMimeType"] == "application/json"
    assert "responseSchema" in cuerpo["generationConfig"]


def test_gemini_llave_invalida():
    transporte = httpx.MockTransport(lambda _: httpx.Response(403, json={}))
    proveedor = GeminiProveedor(
        Settings(GEMINI_API_KEY="mala"), cliente=httpx.Client(transport=transporte)
    )
    with pytest.raises(ProveedorIAError, match="GEMINI_API_KEY"):
        proveedor.diagnosticar(JPEG, "image/jpeg")


def test_gemini_bloqueo_sin_candidatos():
    transporte = httpx.MockTransport(
        lambda _: httpx.Response(200, json={"promptFeedback": {"blockReason": "X"}})
    )
    proveedor = GeminiProveedor(
        Settings(GEMINI_API_KEY="ok"), cliente=httpx.Client(transport=transporte)
    )
    with pytest.raises(ProveedorIAError):
        proveedor.diagnosticar(JPEG, "image/jpeg")

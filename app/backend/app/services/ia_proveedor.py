"""Proveedores de IA para el análisis de calidad de huevos (RF-34).

El servicio de análisis no habla directamente con Google: pide el
diagnóstico a un `ProveedorIA`. Hay dos implementaciones:

* `GeminiProveedor`: envía la foto a la API de Gemini (plan gratuito de
  Google AI Studio) y exige una respuesta JSON con un esquema fijo.
* `DemoProveedor`: devuelve un diagnóstico de ejemplo, marcado como
  demostración. Se usa cuando no hay `GEMINI_API_KEY` (desarrollo,
  pruebas automáticas y sustentaciones sin internet).

Más adelante se puede agregar un proveedor con un modelo propio (por
ejemplo, un clasificador entrenado con las fotos que los avicultores
marquen como correctas) sin tocar la API ni la app.
"""

import base64
import json
import re
from typing import Any, Protocol

import httpx
from app.core.config import Settings
from app.schemas.analisis_ia import DiagnosticoIA
from pydantic import ValidationError

_URL_GEMINI = (
    "https://generativelanguage.googleapis.com/v1beta/models/"
    "{modelo}:generateContent"
)

_TIPOS_ANOMALIA = {
    "grieta",
    "cascara_rota",
    "suciedad",
    "mancha_sangre",
    "deformidad",
    "cascara_rugosa_delgada",
    "tamano_anormal",
    "color_irregular",
    "otro",
}
_GRAVEDADES = {"leve", "moderada", "grave"}
_CALIDADES = {"buena", "regular", "mala"}

PROMPT_ANALISIS = """\
Eres un inspector de calidad de huevos de gallina para pequeñas granjas
avícolas de Colombia. Analiza la fotografía y responde SOLO con JSON
válido que cumpla el esquema indicado. Escribe todos los textos en
español claro, para un avicultor.

Instrucciones:
1. Si la foto no muestra huevos de gallina, responde
   es_imagen_de_huevos=false, huevos_detectados=0, calidad_general="mala",
   puntaje_calidad=0, apto_venta=false, listas vacías y un resumen que
   pida tomar otra foto.
2. Cuenta los huevos visibles (huevos_detectados).
3. Revisa cada huevo y reporta solo las anomalías que realmente veas:
   grieta (fisura en la cáscara), cascara_rota, suciedad (heces, plumas,
   barro o manchas en la cáscara), mancha_sangre (solo si la foto es con
   ovoscopio o luz por detrás), deformidad (forma irregular o alargada),
   cascara_rugosa_delgada (rugosidad, porosidad o cáscara fina),
   tamano_anormal (muy pequeño, muy grande o doble yema aparente),
   color_irregular (decoloración o tono desigual) u otro.
   Para cada una indica gravedad (leve, moderada o grave), cuántos
   huevos afecta y tu confianza entre 0 y 1.
4. No inventes defectos. Las microgrietas y las manchas internas casi
   nunca se ven en una foto normal; si no estás seguro, baja la confianza
   y dilo en el resumen.
5. Clasifica los huevos en los tipos de la granja (distribucion):
   AA (primera calidad, sin defectos, tamaño grande), A (buena calidad,
   pequeñas imperfecciones), B (defectos visibles: rugosidad, manchas,
   tamaño pequeño) y No_apto (roto, cáscara blanda, deformación grave o
   contaminado). La suma debe ser igual a huevos_detectados.
6. calidad_general: "buena" si casi todos son AA o A, "regular" si hay
   varios B, "mala" si hay huevos No_apto o anomalías graves.
   puntaje_calidad de 0 a 100. apto_venta=true si el lote puede venderse
   después de separar los huevos No_apto.
7. Da entre 2 y 5 recomendaciones prácticas y baratas para la granja.
"""

# Esquema en el formato OpenAPI que acepta `responseSchema` de Gemini.
ESQUEMA_RESPUESTA: dict[str, Any] = {
    "type": "OBJECT",
    "properties": {
        "es_imagen_de_huevos": {"type": "BOOLEAN"},
        "huevos_detectados": {"type": "INTEGER"},
        "calidad_general": {
            "type": "STRING",
            "enum": sorted(_CALIDADES),
        },
        "puntaje_calidad": {"type": "INTEGER"},
        "apto_venta": {"type": "BOOLEAN"},
        "resumen": {"type": "STRING"},
        "anomalias": {
            "type": "ARRAY",
            "items": {
                "type": "OBJECT",
                "properties": {
                    "tipo": {"type": "STRING", "enum": sorted(_TIPOS_ANOMALIA)},
                    "descripcion": {"type": "STRING"},
                    "gravedad": {"type": "STRING", "enum": sorted(_GRAVEDADES)},
                    "huevos_afectados": {"type": "INTEGER"},
                    "confianza": {"type": "NUMBER"},
                },
                "required": [
                    "tipo",
                    "descripcion",
                    "gravedad",
                    "huevos_afectados",
                    "confianza",
                ],
            },
        },
        "distribucion": {
            "type": "OBJECT",
            "properties": {
                "AA": {"type": "INTEGER"},
                "A": {"type": "INTEGER"},
                "B": {"type": "INTEGER"},
                "No_apto": {"type": "INTEGER"},
            },
            "required": ["AA", "A", "B", "No_apto"],
        },
        "recomendaciones": {"type": "ARRAY", "items": {"type": "STRING"}},
        "confianza_general": {"type": "NUMBER"},
    },
    "required": [
        "es_imagen_de_huevos",
        "huevos_detectados",
        "calidad_general",
        "puntaje_calidad",
        "apto_venta",
        "resumen",
        "anomalias",
        "distribucion",
        "recomendaciones",
        "confianza_general",
    ],
}


class ProveedorIAError(Exception):
    """El proveedor de IA no pudo entregar un diagnóstico válido.

    El mensaje está redactado para mostrarse al usuario.
    """


class ProveedorIA(Protocol):
    """Contrato que cumple cualquier motor de diagnóstico."""

    nombre: str
    es_demo: bool

    def diagnosticar(self, imagen: bytes, mime_type: str) -> DiagnosticoIA:
        """Analiza la foto y devuelve el diagnóstico estructurado."""
        ...


def _limitar(valor: Any, minimo: float, maximo: float, defecto: float) -> float:
    """Convierte a número y lo acota al rango indicado."""
    try:
        numero = float(valor)
    except (TypeError, ValueError):
        return defecto
    return max(minimo, min(maximo, numero))


def normalizar_diagnostico(datos: dict[str, Any]) -> DiagnosticoIA:
    """Corrige valores fuera de rango y valida el JSON del modelo.

    Los modelos de lenguaje a veces devuelven un número como texto, una
    confianza de 85 en lugar de 0,85 o un tipo de anomalía fuera del
    catálogo. Esta función repara esos casos antes de validar con
    Pydantic, para no perder un diagnóstico útil por un detalle de forma.

    Args:
        datos: Objeto JSON devuelto por el proveedor.

    Returns:
        DiagnosticoIA: Diagnóstico validado.

    Raises:
        ProveedorIAError: Si el JSON no tiene la estructura mínima.
    """
    try:
        anomalias = []
        for item in datos.get("anomalias") or []:
            tipo = str(item.get("tipo", "otro")).strip().lower()
            gravedad = str(item.get("gravedad", "moderada")).strip().lower()
            confianza = _limitar(item.get("confianza"), 0, 100, 0.5)
            anomalias.append(
                {
                    "tipo": tipo if tipo in _TIPOS_ANOMALIA else "otro",
                    "descripcion": str(item.get("descripcion", ""))[:300],
                    "gravedad": gravedad if gravedad in _GRAVEDADES else "moderada",
                    "huevos_afectados": int(
                        _limitar(item.get("huevos_afectados"), 0, 10_000, 0)
                    ),
                    "confianza": confianza / 100 if confianza > 1 else confianza,
                }
            )

        distribucion = datos.get("distribucion") or {}
        calidad = str(datos.get("calidad_general", "regular")).strip().lower()
        confianza_general = _limitar(datos.get("confianza_general"), 0, 100, 0.5)
        normalizado = {
            "es_imagen_de_huevos": bool(datos.get("es_imagen_de_huevos", True)),
            "huevos_detectados": int(
                _limitar(datos.get("huevos_detectados"), 0, 10_000, 0)
            ),
            "calidad_general": calidad if calidad in _CALIDADES else "regular",
            "puntaje_calidad": int(_limitar(datos.get("puntaje_calidad"), 0, 100, 0)),
            "apto_venta": bool(datos.get("apto_venta", False)),
            "resumen": str(datos.get("resumen", "")).strip()[:600]
            or "Sin resumen del modelo.",
            "anomalias": anomalias,
            "distribucion": {
                clave: int(_limitar(distribucion.get(clave), 0, 10_000, 0))
                for clave in ("AA", "A", "B", "No_apto")
            },
            "recomendaciones": [
                str(r).strip()[:300]
                for r in (datos.get("recomendaciones") or [])
                if str(r).strip()
            ][:6],
            "confianza_general": (
                confianza_general / 100 if confianza_general > 1 else confianza_general
            ),
        }
        return DiagnosticoIA.model_validate(normalizado)
    except (AttributeError, TypeError, ValueError, ValidationError) as exc:
        raise ProveedorIAError(
            "La IA devolvió una respuesta incompleta. Intenta de nuevo."
        ) from exc


def extraer_json(texto: str) -> dict[str, Any]:
    """Extrae el objeto JSON de la respuesta, aunque venga con ```json.

    Args:
        texto: Texto devuelto por el modelo.

    Returns:
        dict: El objeto JSON decodificado.

    Raises:
        ProveedorIAError: Si no hay un objeto JSON legible.
    """
    limpio = re.sub(r"^```(?:json)?\s*|\s*```$", "", texto.strip())
    inicio, fin = limpio.find("{"), limpio.rfind("}")
    if inicio == -1 or fin <= inicio:
        raise ProveedorIAError("La IA no devolvió un diagnóstico legible.")
    try:
        datos = json.loads(limpio[inicio : fin + 1])
    except json.JSONDecodeError as exc:
        raise ProveedorIAError("La IA no devolvió un diagnóstico legible.") from exc
    if not isinstance(datos, dict):
        raise ProveedorIAError("La IA no devolvió un diagnóstico legible.")
    return datos


class GeminiProveedor:
    """Diagnóstico con la API de Gemini (Google AI Studio, plan gratuito).

    Usa el endpoint REST `generateContent` con la imagen en línea
    (`inline_data`) y salida JSON forzada (`responseMimeType` +
    `responseSchema`). Si el modelo principal agotó su cupo o no está
    disponible, reintenta una vez con el modelo de respaldo.
    """

    es_demo = False

    def __init__(self, settings: Settings, cliente: httpx.Client | None = None):
        self._llave = settings.GEMINI_API_KEY
        self._modelos = [settings.GEMINI_MODEL]
        if settings.GEMINI_MODEL_RESPALDO and (
            settings.GEMINI_MODEL_RESPALDO != settings.GEMINI_MODEL
        ):
            self._modelos.append(settings.GEMINI_MODEL_RESPALDO)
        self._timeout = settings.IA_TIMEOUT_SEGUNDOS
        self._cliente = cliente
        self.nombre = f"gemini:{settings.GEMINI_MODEL}"

    def _cuerpo(self, imagen: bytes, mime_type: str) -> dict[str, Any]:
        """Arma el cuerpo de la petición generateContent."""
        return {
            "contents": [
                {
                    "role": "user",
                    "parts": [
                        {
                            "inline_data": {
                                "mime_type": mime_type,
                                "data": base64.b64encode(imagen).decode("ascii"),
                            }
                        },
                        {"text": PROMPT_ANALISIS},
                    ],
                }
            ],
            "generationConfig": {
                "temperature": 0.2,
                "responseMimeType": "application/json",
                "responseSchema": ESQUEMA_RESPUESTA,
            },
        }

    def _llamar(self, modelo: str, cuerpo: dict[str, Any]) -> httpx.Response:
        """Envía la petición a un modelo concreto."""
        url = _URL_GEMINI.format(modelo=modelo)
        encabezados = {"x-goog-api-key": self._llave}
        if self._cliente is not None:
            return self._cliente.post(
                url, json=cuerpo, headers=encabezados, timeout=self._timeout
            )
        with httpx.Client(timeout=self._timeout) as cliente:
            return cliente.post(url, json=cuerpo, headers=encabezados)

    def diagnosticar(self, imagen: bytes, mime_type: str) -> DiagnosticoIA:
        """Analiza la foto con Gemini.

        Args:
            imagen: Bytes de la foto (JPEG, PNG o WEBP).
            mime_type: Tipo MIME de la foto.

        Returns:
            DiagnosticoIA: Diagnóstico validado.

        Raises:
            ProveedorIAError: Si ningún modelo entrega una respuesta válida.
        """
        cuerpo = self._cuerpo(imagen, mime_type)
        ultimo_error = "El servicio de IA no está disponible en este momento."
        for modelo in self._modelos:
            try:
                respuesta = self._llamar(modelo, cuerpo)
            except httpx.TimeoutException:
                ultimo_error = "La IA tardó demasiado en responder. Intenta de nuevo."
                continue
            except httpx.HTTPError:
                ultimo_error = "No se pudo conectar con el servicio de IA."
                continue

            if respuesta.status_code in (429, 404, 500, 502, 503, 504):
                # Cupo agotado, modelo retirado o caída: probar el respaldo.
                ultimo_error = (
                    "Se agotó el cupo gratuito de análisis por ahora. "
                    "Intenta más tarde."
                    if respuesta.status_code == 429
                    else "El servicio de IA no está disponible en este momento."
                )
                continue
            if respuesta.status_code in (400, 401, 403):
                raise ProveedorIAError(
                    "El servicio de IA rechazó la solicitud. Revisa la llave "
                    "GEMINI_API_KEY del servidor."
                    if respuesta.status_code in (401, 403)
                    else "La IA no pudo procesar esta imagen. Toma otra foto."
                )
            if respuesta.status_code != 200:
                continue

            texto = self._texto_respuesta(respuesta.json())
            self.nombre = f"gemini:{modelo}"
            return normalizar_diagnostico(extraer_json(texto))

        raise ProveedorIAError(ultimo_error)

    @staticmethod
    def _texto_respuesta(datos: dict[str, Any]) -> str:
        """Saca el texto del primer candidato de generateContent."""
        candidatos = datos.get("candidates") or []
        if not candidatos:
            raise ProveedorIAError(
                "La IA no analizó la imagen. Prueba con otra foto de los huevos."
            )
        partes = (candidatos[0].get("content") or {}).get("parts") or []
        texto = "".join(p.get("text", "") for p in partes if isinstance(p, dict))
        if not texto.strip():
            raise ProveedorIAError(
                "La IA no analizó la imagen. Prueba con otra foto de los huevos."
            )
        return texto


class DemoProveedor:
    """Diagnóstico de ejemplo cuando no hay llave de Gemini configurada.

    Permite probar el flujo completo (foto, diagnóstico, recomendaciones e
    historial) sin internet ni costos. El resumen deja claro que no es un
    análisis real de la foto.
    """

    nombre = "demo"
    es_demo = True

    def diagnosticar(self, imagen: bytes, mime_type: str) -> DiagnosticoIA:
        """Devuelve siempre el mismo diagnóstico de demostración."""
        return DiagnosticoIA(
            es_imagen_de_huevos=True,
            huevos_detectados=12,
            calidad_general="regular",
            puntaje_calidad=72,
            apto_venta=True,
            resumen=(
                "MODO DEMOSTRACIÓN: este resultado es un ejemplo y no analiza "
                "tu foto. Configura GEMINI_API_KEY en el servidor para obtener "
                "un diagnóstico real. Ejemplo: 12 huevos, la mayoría de buena "
                "calidad, con suciedad leve y un huevo con fisura."
            ),
            anomalias=[
                {
                    "tipo": "suciedad",
                    "descripcion": "Manchas de excremento en la cáscara.",
                    "gravedad": "leve",
                    "huevos_afectados": 3,
                    "confianza": 0.8,
                },
                {
                    "tipo": "grieta",
                    "descripcion": "Fisura visible en un extremo del huevo.",
                    "gravedad": "moderada",
                    "huevos_afectados": 1,
                    "confianza": 0.7,
                },
            ],
            distribucion={"AA": 6, "A": 3, "B": 2, "No_apto": 1},
            recomendaciones=[
                "Separa el huevo con fisura y no lo vendas para consumo crudo.",
            ],
            confianza_general=0.75,
        )


def crear_proveedor(settings: Settings) -> ProveedorIA:
    """Elige el proveedor según la configuración del servidor.

    Args:
        settings: Configuración de la aplicación.

    Returns:
        ProveedorIA: Gemini si hay llave; si no, el proveedor de demostración.
    """
    if settings.GEMINI_API_KEY.strip():
        return GeminiProveedor(settings)
    return DemoProveedor()

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

# Tipos de anomalía que la IA puede reportar. Se mantienen en un catálogo
# cerrado para que las recomendaciones personalizadas sean predecibles.
TipoAnomalia = Literal[
    "grieta",
    "cascara_rota",
    "suciedad",
    "mancha_sangre",
    "deformidad",
    "cascara_rugosa_delgada",
    "tamano_anormal",
    "color_irregular",
    "otro",
]
Gravedad = Literal["leve", "moderada", "grave"]
Calidad = Literal["buena", "regular", "mala"]


class AnomaliaIA(BaseModel):
    """Anomalía detectada en uno o varios huevos de la foto."""

    tipo: TipoAnomalia
    descripcion: str = Field(max_length=300)
    gravedad: Gravedad
    huevos_afectados: int = Field(ge=0)
    confianza: float = Field(ge=0, le=1)


class DistribucionTipos(BaseModel):
    """Clasificación estimada de los huevos según los tipos de la granja."""

    AA: int = Field(default=0, ge=0)
    A: int = Field(default=0, ge=0)
    B: int = Field(default=0, ge=0)
    No_apto: int = Field(default=0, ge=0)


class DiagnosticoIA(BaseModel):
    """Respuesta estructurada del proveedor de IA (antes de personalizar)."""

    es_imagen_de_huevos: bool
    huevos_detectados: int = Field(ge=0)
    calidad_general: Calidad
    puntaje_calidad: int = Field(ge=0, le=100)
    apto_venta: bool
    resumen: str = Field(max_length=600)
    anomalias: list[AnomaliaIA] = Field(default_factory=list)
    distribucion: DistribucionTipos = Field(default_factory=DistribucionTipos)
    recomendaciones: list[str] = Field(default_factory=list)
    confianza_general: float = Field(ge=0, le=1)


class EstadoIAResponse(BaseModel):
    """Disponibilidad del módulo para el usuario autenticado."""

    disponible: bool
    plan: str
    modo_demo: bool
    proveedor: str
    limite_diario: int
    usados_hoy: int
    mensaje: str


class AnalisisResponse(BaseModel):
    """Análisis guardado, listo para mostrar en la app."""

    id_analisis: int
    id_camada: int | None
    nombre_camada: str | None
    fecha_analisis: datetime
    tiene_imagen: bool
    resultado_diagnostico: str
    recomendaciones: list[str]
    calidad_general: Calidad | None
    puntaje_calidad: int | None
    apto_venta: bool | None
    huevos_detectados: int | None
    anomalias: list[AnomaliaIA]
    distribucion: DistribucionTipos | None
    confianza_general: float | None
    proveedor_ia: str | None
    modo_demo: bool
    diagnostico_correcto: bool | None


class RetroalimentacionRequest(BaseModel):
    """Opinión del avicultor sobre el diagnóstico recibido."""

    diagnostico_correcto: bool

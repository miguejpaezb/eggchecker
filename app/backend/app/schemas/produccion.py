from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

# Las dos formas de registrar cantidades: huevos sueltos o cubetas (RF-17).
_UNIDAD_PRODUCCION = Literal["unidad", "cubeta"]


class ProduccionCreate(BaseModel):
    """Datos de entrada para registrar la recolección diaria de una camada.

    Las cantidades se reciben por tipo de huevo y, si la unidad es
    'cubeta', el servidor las convierte a huevos (1 cubeta = 30). El
    `total_huevos` nunca se recibe: siempre se calcula en el servidor.
    """

    id_camada: int
    fecha_recoleccion: date
    unidad: _UNIDAD_PRODUCCION = "unidad"
    aa: int = Field(default=0, ge=0)
    a: int = Field(default=0, ge=0)
    b: int = Field(default=0, ge=0)
    no_apto: int = Field(default=0, ge=0)
    observaciones: str | None = None


class ProduccionDetalleResponse(BaseModel):
    """Línea de detalle: cantidad recogida de un tipo de huevo."""

    id_detalle: int
    id_produccion: int
    id_tipo: int
    nombre_tipo: str
    cantidad: int


class ProduccionResponse(BaseModel):
    """Producción tal como se persiste, sin el detalle por tipo."""

    id_produccion: int
    id_usuario: int
    id_camada: int
    fecha_recoleccion: date
    total_huevos: int
    observaciones: str | None

    model_config = ConfigDict(from_attributes=True)


class ProduccionConDetalleResponse(ProduccionResponse):
    """Producción con su desglose por tipo de huevo."""

    detalle: list[ProduccionDetalleResponse]


class ResumenResponse(BaseModel):
    """Acumulados de producción del usuario (RF-18).

    `total_hoy`, `total_semana` y `total_mes` son sumas de `total_huevos`;
    `por_tipo` desglosa el mes actual por nombre de tipo (AA, A, B,
    No_apto), con los cuatro tipos siempre presentes.
    """

    total_hoy: int
    total_semana: int
    total_mes: int
    por_tipo: dict[str, int]


class DisponibleTipoResponse(BaseModel):
    """Huevos disponibles de un tipo: producido menos vendido."""

    id_tipo: int
    nombre_tipo: str
    producido: int
    vendido: int
    disponible: int


class DisponiblesResponse(BaseModel):
    """Inventario de huevos disponibles del usuario (RF-16)."""

    total_disponible: int
    por_tipo: list[DisponibleTipoResponse]

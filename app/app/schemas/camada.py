from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

# Los únicos estados posibles de una camada (DDL chk_camada_estado).
_ESTADOS_CAMADA = Literal["activa", "retirada"]


class CamadaCreate(BaseModel):
    """Datos de entrada para registrar una camada nueva.

    La edad inicial no se recibe: el servidor siempre la fija en 16
    semanas (regla de negocio RF-12).
    """

    nombre_camada: str = Field(min_length=1, max_length=60)
    fecha_ingreso: date
    cantidad_inicial: int = Field(gt=0)
    estado: _ESTADOS_CAMADA = "activa"


class CamadaUpdate(BaseModel):
    """Datos editables de una camada activa.

    Solo se puede cambiar el nombre y, dentro de las primeras 24 horas, la
    cantidad inicial. La fecha de ingreso, la edad, la cantidad actual y el
    estado se gestionan con acciones específicas.
    """

    nombre_camada: str | None = Field(default=None, min_length=1, max_length=60)
    cantidad_inicial: int | None = Field(default=None, gt=0)

    model_config = ConfigDict(extra="forbid")


class MortalidadRequest(BaseModel):
    """Cantidad de aves que mueren y se descuentan de la camada."""

    cantidad: int = Field(gt=0)


class CamadaResponse(BaseModel):
    """Camada tal como se persiste, sin campos calculados."""

    id_camada: int
    id_usuario: int
    nombre_camada: str
    fecha_ingreso: date
    cantidad_inicial: int
    cantidad_actual: int
    estado: str
    edad_semanas: int
    fecha_proximo_aviso: date | None
    fecha_creacion: datetime
    requiere_decision: bool
    puede_editar_inicial: bool

    model_config = ConfigDict(from_attributes=True)


class CamadaDetalleResponse(CamadaResponse):
    """Camada con edad y fecha de retiro estimada (calculadas en el servicio)."""

    edad_dias: int
    fecha_retiro_estimada: date

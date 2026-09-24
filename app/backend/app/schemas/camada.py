from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

# Los únicos estados posibles de una camada (DDL chk_camada_estado).
_ESTADOS_CAMADA = Literal["activa", "retirada"]


class CamadaCreate(BaseModel):
    """Datos de entrada para registrar una camada nueva."""

    nombre_camada: str = Field(min_length=1, max_length=60)
    fecha_ingreso: date
    cantidad_inicial: int = Field(gt=0)
    estado: _ESTADOS_CAMADA = "activa"


class CamadaUpdate(BaseModel):
    """Datos parciales para actualizar una camada existente."""

    nombre_camada: str | None = Field(default=None, min_length=1, max_length=60)
    fecha_ingreso: date | None = None
    cantidad_inicial: int | None = Field(default=None, gt=0)
    cantidad_actual: int | None = Field(default=None, ge=0)
    estado: _ESTADOS_CAMADA | None = None


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

    model_config = ConfigDict(from_attributes=True)


class CamadaDetalleResponse(CamadaResponse):
    """Camada con edad y fecha de retiro estimada (calculadas en el servicio)."""

    edad_dias: int
    fecha_retiro_estimada: date

from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field


class AlimentacionCreate(BaseModel):
    """Datos para registrar el alimento consumido por una camada.

    La cantidad se expresa en la unidad de medida del insumo y no puede
    superar su stock actual (lo valida el servicio).
    """

    id_insumo: int
    cantidad: Decimal = Field(gt=0, max_digits=10, decimal_places=2)
    observaciones: str | None = Field(default=None, max_length=200)


class AlimentacionResponse(BaseModel):
    """Registro de alimentación con los datos del insumo y la camada."""

    id_alimentacion: int
    id_camada: int
    nombre_camada: str
    id_insumo: int
    nombre_insumo: str
    unidad_medida: str
    cantidad: Decimal
    fecha_hora: datetime
    observaciones: str | None

from datetime import datetime
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

# Los únicos tipos de movimiento posibles (DDL chk_movimiento_tipo).
_TIPO_MOVIMIENTO = Literal["entrada", "salida"]


class CategoriaCreate(BaseModel):
    """Datos de entrada para registrar una categoría de insumo nueva."""

    nombre_categ: str = Field(min_length=1, max_length=60)
    descripcion: str | None = Field(default=None, max_length=200)


class CategoriaUpdate(BaseModel):
    """Datos parciales para actualizar una categoría existente."""

    nombre_categ: str | None = Field(default=None, min_length=1, max_length=60)
    descripcion: str | None = Field(default=None, max_length=200)


class CategoriaResponse(BaseModel):
    """Categoría tal como se persiste en el catálogo global."""

    id_categoria: int
    nombre_categ: str
    descripcion: str | None

    model_config = ConfigDict(from_attributes=True)


class InsumoCreate(BaseModel):
    """Datos de entrada para registrar un insumo nuevo del usuario.

    El stock actual y el umbral mínimo deben ser mayores que cero; los
    decimales se envían con punto.
    """

    id_categoria: int
    nombre_insumo: str = Field(min_length=1, max_length=80)
    unidad_medida: str = Field(min_length=1, max_length=20)
    stock_actual: Decimal = Field(gt=0)
    umbral_minimo: Decimal = Field(gt=0)


class InsumoUpdate(BaseModel):
    """Datos editables de un insumo existente.

    Solo se puede cambiar el nombre, la unidad de medida y el umbral
    mínimo. El stock nunca se modifica aquí: solo cambia vía movimientos.
    """

    nombre_insumo: str | None = Field(default=None, min_length=1, max_length=80)
    unidad_medida: str | None = Field(default=None, min_length=1, max_length=20)
    umbral_minimo: Decimal | None = Field(default=None, gt=0)

    model_config = ConfigDict(extra="forbid")


class InsumoResponse(BaseModel):
    """Insumo tal como se persiste, sin campos calculados."""

    id_insumo: int
    id_usuario: int
    id_categoria: int
    nombre_insumo: str
    unidad_medida: str
    stock_actual: Decimal
    umbral_minimo: Decimal
    activo: bool
    descontinuado: bool

    model_config = ConfigDict(from_attributes=True)


class MovimientoCreate(BaseModel):
    """Datos de entrada para registrar un movimiento de stock."""

    tipo_movimiento: _TIPO_MOVIMIENTO
    cantidad: Decimal = Field(gt=0)
    observaciones: str | None = None


class MovimientoResponse(BaseModel):
    """Movimiento registrado con el stock resultante del insumo."""

    id_movimiento: int
    id_insumo: int
    tipo_movimiento: str
    cantidad: Decimal
    fecha_movimiento: datetime
    observaciones: str | None
    stock_resultante: Decimal


class MovimientoHistorialResponse(BaseModel):
    """Movimiento tal como se persiste, para el historial del insumo."""

    id_movimiento: int
    id_insumo: int
    tipo_movimiento: str
    cantidad: Decimal
    fecha_movimiento: datetime
    observaciones: str | None

    model_config = ConfigDict(from_attributes=True)


class AlertaInsumoResponse(BaseModel):
    """Insumo bajo su umbral mínimo con el déficit calculado."""

    id_insumo: int
    categoria: str
    nombre_insumo: str
    stock_actual: Decimal
    umbral_minimo: Decimal
    deficit: Decimal

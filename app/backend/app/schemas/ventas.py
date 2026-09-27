from datetime import date
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

# Los estados de un pedido (DDL chk_pedido_estado).
ESTADO_PEDIDO = Literal["pendiente", "enviado", "recibido", "cancelado"]
# Estados a los que se puede avanzar manualmente (no incluye cancelado).
AVANCE_ESTADO = Literal["enviado", "recibido"]


def _validar_telefono(valor: str | None) -> str | None:
    """Normaliza el teléfono: vacío -> None y solo dígitos 0-9.

    Args:
        valor: Teléfono recibido en la petición, o None.

    Returns:
        El teléfono sin espacios si es válido, o None si viene vacío.

    Raises:
        ValueError: Si contiene caracteres distintos de dígitos.
    """
    if valor is None:
        return None
    limpio = valor.strip()
    if not limpio:
        return None
    if not (limpio.isascii() and limpio.isdigit()):
        raise ValueError("El teléfono solo puede contener números")
    return limpio


class ClienteCreate(BaseModel):
    """Datos de entrada para registrar un cliente nuevo."""

    nombre_cliente: str = Field(min_length=1, max_length=80)
    telefono: str | None = Field(default=None, max_length=20)
    direccion: str | None = Field(default=None, max_length=200)

    @field_validator("telefono")
    @classmethod
    def _telefono_numerico(cls, valor: str | None) -> str | None:
        """Valida que el teléfono solo contenga dígitos."""
        return _validar_telefono(valor)


class ClienteUpdate(BaseModel):
    """Datos editables de un cliente existente.

    Solo se puede cambiar el nombre, el teléfono y la dirección; el estado
    (activo/suspendido) se gestiona con las acciones de suspender/activar.
    """

    nombre_cliente: str | None = Field(default=None, min_length=1, max_length=80)
    telefono: str | None = Field(default=None, max_length=20)
    direccion: str | None = Field(default=None, max_length=200)

    model_config = ConfigDict(extra="forbid")

    @field_validator("telefono")
    @classmethod
    def _telefono_numerico(cls, valor: str | None) -> str | None:
        """Valida que el teléfono solo contenga dígitos."""
        return _validar_telefono(valor)


class ClienteResponse(BaseModel):
    """Cliente tal como se persiste en la base de datos."""

    id_cliente: int
    id_usuario: int
    nombre_cliente: str
    telefono: str | None
    direccion: str | None
    fecha_ultima_compra: date | None
    activo: bool

    model_config = ConfigDict(from_attributes=True)


class ClienteEliminarRequest(BaseModel):
    """Confirmación de borrado: exige la contraseña del usuario dueño."""

    contrasena: str = Field(min_length=1)


class PedidoDetalleCreate(BaseModel):
    """Línea de un pedido: tipo, cantidad entera y precio por unidad."""

    id_tipo: int
    cantidad: int = Field(gt=0)
    precio_unitario: Decimal = Field(gt=0)


class PedidoCreate(BaseModel):
    """Datos de entrada para registrar un pedido nuevo.

    La cantidad de cada detalle debe ser un entero positivo y no superar
    el stock disponible del tipo; el total y la fecha los fija el
    servidor.
    """

    id_cliente: int
    detalles: list[PedidoDetalleCreate] = Field(min_length=1)


class PedidoUpdate(BaseModel):
    """Datos editables de un pedido en estado pendiente."""

    id_cliente: int | None = None
    detalles: list[PedidoDetalleCreate] | None = Field(default=None, min_length=1)


class CambiarEstadoRequest(BaseModel):
    """Avance manual del estado de un pedido (sin incluir cancelado)."""

    estado: AVANCE_ESTADO


class PedidoEliminarRequest(BaseModel):
    """Confirmación de borrado del pedido: exige la contraseña del usuario."""

    contrasena: str = Field(min_length=1)


class PedidoDetalleResponse(BaseModel):
    """Línea de pedido con el nombre del tipo y su subtotal."""

    id_detalle_pedido: int
    id_tipo: int
    nombre_tipo: str
    cantidad: int
    precio_unitario: Decimal
    subtotal: Decimal

    model_config = ConfigDict(from_attributes=True)


class PedidoResponse(BaseModel):
    """Pedido con los datos del cliente y su detalle completo."""

    id_pedido: int
    id_cliente: int
    id_usuario: int
    cliente_nombre: str
    cliente_direccion: str | None
    fecha_pedido: date
    estado_pedido: str
    valor_total: Decimal
    unidades_totales: int
    detalles: list[PedidoDetalleResponse]


class StockTipoResponse(BaseModel):
    """Existencia disponible de un tipo de huevo y su valor por unidad."""

    id_tipo: int
    nombre_tipo: str
    cantidad_actual: int
    valor_unidad: Decimal


class StockResponse(BaseModel):
    """Stock total disponible y el detalle por tipo de huevo."""

    total_disponible: int
    por_tipo: list[StockTipoResponse]

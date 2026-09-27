from datetime import date

from pydantic import BaseModel, ConfigDict, Field, field_validator


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

from datetime import date

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

# El plan de suscripción hoy solo admite estos dos valores (DDL y seed).
_PLANES_VALIDOS = ("gratuito", "premium")


class RegistroRequest(BaseModel):
    """Datos de entrada para registrar un avicultor nuevo."""

    nombre_completo: str = Field(min_length=2, max_length=100)
    correo_electronico: EmailStr
    contrasena: str = Field(min_length=8, max_length=72)
    telefono: str | None = None
    plan_suscripcion: str = "gratuito"

    @field_validator("contrasena")
    @classmethod
    def _contrasena_segura(cls, valor: str) -> str:
        """Exige una contraseña sin espacios, con mayúscula, número y símbolo."""
        if any(c.isspace() for c in valor):
            raise ValueError("La contraseña no puede contener espacios en blanco")
        if not any(c.isupper() for c in valor):
            raise ValueError("Debe contener al menos una mayúscula")
        if not any(c.isdigit() for c in valor):
            raise ValueError("Debe contener al menos un número")
        if not any(not c.isalnum() for c in valor):
            raise ValueError("Debe contener al menos un símbolo")
        return valor

    @field_validator("plan_suscripcion")
    @classmethod
    def _plan_conocido(cls, valor: str) -> str:
        """Rechaza planes que la BD de hoy no soporta."""
        if valor not in _PLANES_VALIDOS:
            raise ValueError(f"Plan no soportado: {valor}")
        return valor


class LoginRequest(BaseModel):
    """Datos de entrada para iniciar sesión."""

    correo_electronico: EmailStr
    contrasena: str


class RecuperarRequest(BaseModel):
    """Datos de entrada para solicitar la recuperación de contraseña."""

    correo_electronico: EmailStr


class TokenResponse(BaseModel):
    """Token JWT devuelto tras un inicio de sesión exitoso."""

    access_token: str
    token_type: str = "bearer"


class UsuarioResponse(BaseModel):
    """Perfil base del usuario; nunca expone el hash de la contraseña."""

    id_usuario: int
    nombre_completo: str
    correo_electronico: EmailStr
    telefono: str | None
    plan_suscripcion: str
    fecha_registro: date
    activo: bool

    model_config = ConfigDict(from_attributes=True)


class RecuperarResponse(BaseModel):
    """Respuesta del flujo de recuperación (mock en v1)."""

    mensaje: str
    token_recuperacion: str


class PerfilResponse(UsuarioResponse):
    """Perfil del usuario autenticado con los límites de su plan."""

    plan: str
    aves_max: int | None
    clientes_max: int | None
    ia_incluida: bool
    aves_actuales: int
    clientes_actuales: int

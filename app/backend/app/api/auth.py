from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.auth import (
    LoginRequest,
    RecuperarRequest,
    RecuperarResponse,
    RegistroRequest,
    TokenResponse,
    UsuarioResponse,
)
from app.services import auth_service

router = APIRouter(tags=["auth"])


@router.post(
    "/auth/registro",
    response_model=UsuarioResponse,
    status_code=status.HTTP_201_CREATED,
)
def registro(datos: RegistroRequest, db: Session = Depends(get_db)) -> UsuarioResponse:
    """Registra un avicultor nuevo en el sistema.

    Args:
        datos: Datos validados del registro.
        db: Sesión de base de datos.

    Returns:
        UsuarioResponse: El usuario creado, sin el hash de contraseña.
    """
    return auth_service.registrar_usuario(db, datos)


@router.post("/auth/login", response_model=TokenResponse)
def login(datos: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    """Inicia sesión y devuelve un token JWT de acceso.

    Args:
        datos: Credenciales del usuario.
        db: Sesión de base de datos.

    Returns:
        TokenResponse: Token de acceso Bearer.
    """
    token = auth_service.autenticar_usuario(db, datos)
    return TokenResponse(access_token=token)


@router.post("/auth/recuperar", response_model=RecuperarResponse)
def recuperar(
    datos: RecuperarRequest, db: Session = Depends(get_db)
) -> RecuperarResponse:
    """Solicita el envío del enlace de recuperación de contraseña.

    Args:
        datos: Correo del usuario que solicita la recuperación.
        db: Sesión de base de datos.

    Returns:
        RecuperarResponse: Mensaje de confirmación (sin token).

    Raises:
        HTTPException: 404 si el correo no existe; 500 si falla el envío.
    """
    auth_service.solicitar_recuperacion(db, datos)
    return RecuperarResponse(
        mensaje="Se ha enviado un enlace de recuperación a tu correo"
    )

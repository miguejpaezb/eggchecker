from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.auth import (
    CambiarContrasenaRequest,
    MensajeResponse,
    NotificacionesUpdateRequest,
    PerfilResponse,
    PerfilUpdateRequest,
)
from app.services import auth_service

router = APIRouter(tags=["usuarios"])


@router.get("/usuarios/me", response_model=PerfilResponse)
def perfil_usuario(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PerfilResponse:
    """Consulta el perfil del usuario autenticado y los límites de su plan.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PerfilResponse: Perfil con límites, uso e histórico del usuario.
    """
    return PerfilResponse(**auth_service.obtener_perfil(db, usuario))


@router.put("/usuarios/me", response_model=PerfilResponse)
def actualizar_perfil(
    datos: PerfilUpdateRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PerfilResponse:
    """Actualiza los datos personales y de la granja del usuario.

    Args:
        datos: Datos editables del perfil.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PerfilResponse: Perfil con los cambios aplicados.

    Raises:
        HTTPException: 401 si cambia el correo sin la contraseña actual;
            409 si el nuevo correo ya está registrado.
    """
    return PerfilResponse(**auth_service.actualizar_perfil(db, usuario, datos))


@router.put("/usuarios/me/notificaciones", response_model=PerfilResponse)
def actualizar_notificaciones(
    datos: NotificacionesUpdateRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PerfilResponse:
    """Guarda las preferencias de alertas del usuario.

    Args:
        datos: Preferencias de notificaciones.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PerfilResponse: Perfil con las preferencias actualizadas.
    """
    return PerfilResponse(**auth_service.actualizar_notificaciones(db, usuario, datos))


@router.put("/usuarios/me/contrasena", response_model=MensajeResponse)
def cambiar_contrasena(
    datos: CambiarContrasenaRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> MensajeResponse:
    """Cambia la contraseña del usuario autenticado.

    Args:
        datos: Contraseña actual y nueva.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        MensajeResponse: Confirmación del cambio de contraseña.

    Raises:
        HTTPException: 401 si la contraseña actual no coincide.
    """
    auth_service.cambiar_contrasena(db, usuario, datos)
    return MensajeResponse(mensaje="Contraseña actualizada correctamente")

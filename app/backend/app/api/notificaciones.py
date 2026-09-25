from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.notificacion import NotificacionResponse
from app.services import notificacion_service

router = APIRouter(tags=["notificaciones"])


@router.get("/notificaciones", response_model=list[NotificacionResponse])
def listar_notificaciones(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[NotificacionResponse]:
    """Lista las notificaciones del usuario autenticado.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[NotificacionResponse]: Notificaciones, no leídas primero.
    """
    return notificacion_service.listar_notificaciones(db, usuario)


@router.post(
    "/notificaciones/leer-todas",
    status_code=status.HTTP_204_NO_CONTENT,
)
def marcar_todas_leidas(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Marca como leídas todas las notificaciones del usuario.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
    """
    notificacion_service.marcar_todas_leidas(db, usuario)


@router.post(
    "/notificaciones/{id_notificacion}/leer",
    response_model=NotificacionResponse,
)
def marcar_notificacion_leida(
    id_notificacion: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> NotificacionResponse:
    """Marca una notificación del usuario como leída.

    Args:
        id_notificacion: Identificador de la notificación.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        NotificacionResponse: La notificación marcada como leída.
    """
    return notificacion_service.marcar_leida(db, usuario, id_notificacion)


@router.delete(
    "/notificaciones/{id_notificacion}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_notificacion(
    id_notificacion: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Elimina una notificación del usuario.

    Args:
        id_notificacion: Identificador de la notificación a eliminar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
    """
    notificacion_service.eliminar_notificacion(db, usuario, id_notificacion)

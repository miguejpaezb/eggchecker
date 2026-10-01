from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.auth import PerfilResponse
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
        PerfilResponse: Perfil con límites y uso actual de aves y clientes.
    """
    return PerfilResponse(**auth_service.obtener_perfil(db, usuario))

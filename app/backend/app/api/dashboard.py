from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.dashboard import DashboardResponse
from app.services import dashboard_service

router = APIRouter(tags=["dashboard"])


@router.get("/dashboard", response_model=DashboardResponse)
def obtener_dashboard(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> DashboardResponse:
    """Devuelve los indicadores agregados del dashboard del usuario.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        DashboardResponse: KPI, resumen semanal, alertas y pedidos recientes.
    """
    return dashboard_service.obtener_dashboard(db, usuario)

from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from fastapi.responses import Response
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.reporte import AGRUPACION, ReporteConsolidadoResponse
from app.services import reporte_pdf, reporte_service

router = APIRouter(tags=["reportes"])


def _resolver_periodo(desde: date | None, hasta: date | None) -> tuple[date, date]:
    """Completa el rango por defecto: mes actual cuando no se envía."""
    hoy = date.today()
    inicio = desde or hoy.replace(day=1)
    fin = hasta or hoy
    return inicio, fin


@router.get(
    "/reportes/consolidado",
    response_model=ReporteConsolidadoResponse,
)
def obtener_consolidado(
    desde: Annotated[date | None, Query()] = None,
    hasta: Annotated[date | None, Query()] = None,
    agrupacion: Annotated[AGRUPACION, Query()] = "dia",
    camada: Annotated[int | None, Query()] = None,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ReporteConsolidadoResponse:
    """Devuelve el reporte de rentabilidad consolidado del período (RF-37).

    Args:
        desde: Fecha inicial del período; por defecto el primer día del mes.
        hasta: Fecha final del período; por defecto hoy.
        agrupacion: Agrupación de las series ('dia', 'semana' o 'mes').
        camada: Camada a la que acotar el reporte, si se elige una.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ReporteConsolidadoResponse: Producción, ventas, gastos, ganancia,
        salud, alimento e inventario del período.
    """
    inicio, fin = _resolver_periodo(desde, hasta)
    return reporte_service.obtener_consolidado(
        db, usuario, inicio, fin, agrupacion, camada
    )


@router.get("/reportes/pdf")
def exportar_pdf(
    desde: Annotated[date | None, Query()] = None,
    hasta: Annotated[date | None, Query()] = None,
    agrupacion: Annotated[AGRUPACION, Query()] = "dia",
    camada: Annotated[int | None, Query()] = None,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> Response:
    """Exporta el reporte de rentabilidad consolidado a PDF (RF-38).

    Args:
        desde: Fecha inicial del período; por defecto el primer día del mes.
        hasta: Fecha final del período; por defecto hoy.
        agrupacion: Agrupación de las series ('dia', 'semana' o 'mes').
        camada: Camada a la que acotar el reporte, si se elige una.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        Response: El PDF del reporte para descargar.
    """
    inicio, fin = _resolver_periodo(desde, hasta)
    reporte = reporte_service.obtener_consolidado(
        db, usuario, inicio, fin, agrupacion, camada
    )
    contenido = reporte_pdf.generar_pdf(reporte)
    sufijo = f"_{reporte.camada.nombre_camada}" if reporte.camada else ""
    nombre = f"reporte_rentabilidad_{inicio}_{fin}{sufijo}.pdf"
    return Response(
        content=contenido,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{nombre}"'},
    )

from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.produccion_diaria import ProduccionDiaria
from app.models.usuario import Usuario
from app.schemas.produccion import (
    DisponiblesResponse,
    ProduccionConDetalleResponse,
    ProduccionCreate,
    ProduccionDetalleResponse,
    ProduccionResponse,
    ResumenResponse,
)
from app.services import produccion_service

router = APIRouter(tags=["produccion"])


def _con_detalle(produccion: ProduccionDiaria) -> ProduccionConDetalleResponse:
    """Arma la respuesta de producción con su detalle por tipo."""
    return ProduccionConDetalleResponse(
        id_produccion=produccion.id_produccion,
        id_usuario=produccion.id_usuario,
        id_camada=produccion.id_camada,
        fecha_recoleccion=produccion.fecha_recoleccion,
        total_huevos=produccion.total_huevos,
        observaciones=produccion.observaciones,
        detalle=[
            ProduccionDetalleResponse(
                id_detalle=detalle.id_detalle,
                id_produccion=detalle.id_produccion,
                id_tipo=detalle.id_tipo,
                nombre_tipo=detalle.tipo.nombre_tipo,
                cantidad=detalle.cantidad,
            )
            for detalle in produccion.detalles
        ],
    )


@router.post(
    "/produccion",
    response_model=ProduccionConDetalleResponse,
    status_code=status.HTTP_201_CREATED,
)
def registrar_produccion(
    datos: ProduccionCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ProduccionConDetalleResponse:
    """Registra la recolección diaria de una camada del usuario.

    Args:
        datos: Cantidades por tipo, fecha y unidad de la recolección.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ProduccionConDetalleResponse: La producción creada con su detalle.
    """
    produccion = produccion_service.registrar_produccion(db, usuario, datos)
    return _con_detalle(produccion)


@router.get("/produccion", response_model=list[ProduccionResponse])
def listar_produccion(
    camada: Annotated[int | None, Query()] = None,
    fecha: Annotated[date | None, Query()] = None,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[ProduccionResponse]:
    """Lista la producción del usuario, opcionalmente filtrada.

    Args:
        camada: Camada por la que filtrar la producción.
        fecha: Fecha de recolección por la que filtrar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[ProduccionResponse]: Producciones que cumplen los filtros.
    """
    return produccion_service.listar_produccion(
        db, usuario, camada_id=camada, fecha=fecha
    )


@router.get("/produccion/resumen", response_model=ResumenResponse)
def obtener_resumen(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ResumenResponse:
    """Devuelve los acumulados de producción (día, semana y mes).

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ResumenResponse: Totales acumulados y desglose del mes por tipo.
    """
    return ResumenResponse(**produccion_service.obtener_resumen(db, usuario))


@router.get(
    "/produccion/{id_produccion}",
    response_model=ProduccionConDetalleResponse,
)
def obtener_produccion(
    id_produccion: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ProduccionConDetalleResponse:
    """Obtiene una producción del usuario con su detalle por tipo.

    Args:
        id_produccion: Identificador de la producción a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ProduccionConDetalleResponse: La producción con su detalle.
    """
    produccion = produccion_service.obtener_produccion(db, usuario, id_produccion)
    return _con_detalle(produccion)


@router.get("/huevos/disponibles", response_model=DisponiblesResponse)
def obtener_disponibles(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> DisponiblesResponse:
    """Devuelve el inventario de huevos disponibles del usuario.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        DisponiblesResponse: Total disponible y detalle por tipo.
    """
    return DisponiblesResponse(**produccion_service.obtener_disponibles(db, usuario))

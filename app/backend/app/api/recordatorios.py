from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.recordatorio import (
    DosisResponse,
    RecordatorioRequest,
    RecordatorioResponse,
)
from app.services import recordatorio_service

router = APIRouter(tags=["recordatorios"])


@router.get("/recordatorios", response_model=list[RecordatorioResponse])
def listar_recordatorios(
    id_camada: Annotated[int | None, Query()] = None,
    incluir_globales: Annotated[bool, Query()] = True,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[RecordatorioResponse]:
    """Lista los recordatorios sanitarios activos del avicultor.

    Args:
        id_camada: Filtra los recordatorios de una camada.
        incluir_globales: Suma los recordatorios de toda la granja.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[RecordatorioResponse]: Recordatorios con su próxima dosis.
    """
    return recordatorio_service.listar_recordatorios(
        db, usuario, id_camada, incluir_globales
    )


@router.post(
    "/recordatorios",
    response_model=RecordatorioResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_recordatorio(
    datos: RecordatorioRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> RecordatorioResponse:
    """Crea un recordatorio de vacunación o de vitaminas.

    Args:
        datos: Regla validada (sin `id_camada` aplica a toda la granja).
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        RecordatorioResponse: El recordatorio creado.
    """
    return recordatorio_service.crear_recordatorio(db, usuario, datos)


@router.get(
    "/recordatorios/{id_recordatorio}",
    response_model=RecordatorioResponse,
)
def obtener_recordatorio(
    id_recordatorio: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> RecordatorioResponse:
    """Devuelve un recordatorio del avicultor.

    Args:
        id_recordatorio: Identificador del recordatorio.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        RecordatorioResponse: El recordatorio consultado.
    """
    regla = recordatorio_service.obtener_detalle(db, usuario, id_recordatorio)
    return regla


@router.put(
    "/recordatorios/{id_recordatorio}",
    response_model=RecordatorioResponse,
)
def actualizar_recordatorio(
    id_recordatorio: int,
    datos: RecordatorioRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> RecordatorioResponse:
    """Reemplaza un recordatorio y recalcula sus dosis futuras.

    Args:
        id_recordatorio: Identificador del recordatorio.
        datos: Regla completa con los cambios.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        RecordatorioResponse: El recordatorio actualizado.
    """
    return recordatorio_service.actualizar_recordatorio(
        db, usuario, id_recordatorio, datos
    )


@router.delete(
    "/recordatorios/{id_recordatorio}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_recordatorio(
    id_recordatorio: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Desactiva un recordatorio y descarta sus dosis pendientes.

    Args:
        id_recordatorio: Identificador del recordatorio.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
    """
    recordatorio_service.eliminar_recordatorio(db, usuario, id_recordatorio)


@router.get(
    "/recordatorios/{id_recordatorio}/dosis",
    response_model=list[DosisResponse],
)
def listar_dosis(
    id_recordatorio: int,
    desde: Annotated[date | None, Query()] = None,
    hasta: Annotated[date | None, Query()] = None,
    solo_pendientes: Annotated[bool, Query()] = False,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[DosisResponse]:
    """Lista las aplicaciones programadas de un recordatorio.

    Args:
        id_recordatorio: Identificador del recordatorio.
        desde: Fecha mínima del rango.
        hasta: Fecha máxima del rango.
        solo_pendientes: Excluye las dosis ya aplicadas.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[DosisResponse]: Dosis ordenadas por fecha y camada.
    """
    return recordatorio_service.listar_dosis(
        db, usuario, id_recordatorio, desde, hasta, solo_pendientes
    )

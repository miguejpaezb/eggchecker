from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.camada import (
    CamadaCreate,
    CamadaDetalleResponse,
    CamadaResponse,
    CamadaUpdate,
    MortalidadRequest,
)
from app.services import camada_service

router = APIRouter(tags=["camadas"])


@router.post(
    "/camadas",
    response_model=CamadaResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_camada(
    datos: CamadaCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Registra una camada nueva para el usuario autenticado.

    Args:
        datos: Datos validados de la camada a crear.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada creada.
    """
    return camada_service.crear_camada(db, usuario, datos)


@router.get("/camadas", response_model=list[CamadaResponse])
def listar_camadas(
    estado: str | None = Query(default=None),
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[CamadaResponse]:
    """Lista las camadas del usuario, opcionalmente filtradas por estado.

    Args:
        estado: Estado por el que filtrar ('activa' o 'retirada').
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[CamadaResponse]: Camadas que cumplen el filtro.
    """
    return camada_service.listar_camadas(db, usuario, estado)


@router.get("/camadas/{id_camada}", response_model=CamadaDetalleResponse)
def obtener_camada(
    id_camada: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaDetalleResponse:
    """Obtiene una camada del usuario con su edad y retiro estimado.

    Args:
        id_camada: Identificador de la camada a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaDetalleResponse: Camada con campos calculados.
    """
    camada = camada_service.obtener_camada(db, usuario, id_camada)
    return CamadaDetalleResponse(
        id_camada=camada.id_camada,
        id_usuario=camada.id_usuario,
        nombre_camada=camada.nombre_camada,
        fecha_ingreso=camada.fecha_ingreso,
        cantidad_inicial=camada.cantidad_inicial,
        cantidad_actual=camada.cantidad_actual,
        estado=camada.estado,
        edad_dias=camada_service.calcular_edad_dias(camada),
        fecha_retiro_estimada=camada_service.calcular_fecha_retiro(camada),
    )


@router.patch("/camadas/{id_camada}", response_model=CamadaResponse)
def actualizar_camada(
    id_camada: int,
    datos: CamadaUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Actualiza los campos enviados de una camada del usuario.

    Args:
        id_camada: Identificador de la camada a actualizar.
        datos: Campos que se desean modificar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada actualizada.
    """
    return camada_service.actualizar_camada(db, usuario, id_camada, datos)


@router.post("/camadas/{id_camada}/mortalidad", response_model=CamadaResponse)
def registrar_mortalidad_camada(
    id_camada: int,
    datos: MortalidadRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Registra mortalidad y descuenta la cantidad actual de la camada.

    Args:
        id_camada: Identificador de la camada afectada.
        datos: Cantidad de aves muertas a descontar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada con la cantidad actual descontada.
    """
    return camada_service.registrar_mortalidad(db, usuario, id_camada, datos)


@router.get("/camadas/{id_camada}/edad")
def consultar_edad_camada(
    id_camada: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> dict:
    """Consulta la edad en días y el retiro estimado de una camada.

    Args:
        id_camada: Identificador de la camada a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        dict: Edad en días, fecha de ingreso y fecha de retiro estimada.
    """
    camada = camada_service.obtener_camada(db, usuario, id_camada)
    return {
        "edad_dias": camada_service.calcular_edad_dias(camada),
        "fecha_ingreso": camada.fecha_ingreso,
        "fecha_retiro_estimada": camada_service.calcular_fecha_retiro(camada),
    }

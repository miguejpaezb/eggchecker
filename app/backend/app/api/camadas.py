from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.camada import Camada
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


def _detalle(camada: Camada) -> CamadaDetalleResponse:
    """Arma la respuesta de detalle con los campos calculados."""
    return CamadaDetalleResponse(
        id_camada=camada.id_camada,
        id_usuario=camada.id_usuario,
        nombre_camada=camada.nombre_camada,
        fecha_ingreso=camada.fecha_ingreso,
        cantidad_inicial=camada.cantidad_inicial,
        cantidad_actual=camada.cantidad_actual,
        estado=camada.estado,
        edad_semanas=camada.edad_semanas,
        fecha_proximo_aviso=camada.fecha_proximo_aviso,
        fecha_creacion=camada.fecha_creacion,
        requiere_decision=camada.requiere_decision,
        puede_editar_inicial=camada.puede_editar_inicial,
        edad_dias=camada_service.calcular_edad_dias(camada),
        fecha_retiro_estimada=camada_service.calcular_fecha_retiro(camada),
    )


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


@router.get("/camadas/alertas", response_model=list[CamadaResponse])
def listar_alertas(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[CamadaResponse]:
    """Lista las camadas activas que piden decisión (aviso semanal).

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[CamadaResponse]: Camadas con decisión pendiente.
    """
    return camada_service.listar_alertas(db, usuario)


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
    return _detalle(camada_service.obtener_camada(db, usuario, id_camada))


@router.patch("/camadas/{id_camada}", response_model=CamadaResponse)
def actualizar_camada(
    id_camada: int,
    datos: CamadaUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Edita el nombre y, dentro de 24 horas, la cantidad inicial.

    Args:
        id_camada: Identificador de la camada a actualizar.
        datos: Campos editables que se desean modificar.
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


@router.post("/camadas/{id_camada}/avanzar-semana", response_model=CamadaResponse)
def avanzar_semana_camada(
    id_camada: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Suma una semana de vida a una camada activa.

    Args:
        id_camada: Identificador de la camada a avanzar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada con una semana más de edad.
    """
    return camada_service.avanzar_semana(db, usuario, id_camada)


@router.post("/camadas/{id_camada}/seguir-activa", response_model=CamadaResponse)
def seguir_activa_camada(
    id_camada: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Posponer una semana la decisión de una camada que pide aviso.

    Args:
        id_camada: Identificador de la camada que sigue activa.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada con el próximo aviso agendado.
    """
    return camada_service.seguir_activa(db, usuario, id_camada)


@router.post("/camadas/{id_camada}/descartar", response_model=CamadaResponse)
def descartar_camada(
    id_camada: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CamadaResponse:
    """Descarta una camada activa (estado 'retirada', irreversible).

    Args:
        id_camada: Identificador de la camada a descartar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CamadaResponse: La camada con estado 'retirada'.
    """
    return camada_service.descartar_camada(db, usuario, id_camada)


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

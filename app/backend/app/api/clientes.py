from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.ventas import (
    ClienteCreate,
    ClienteEliminarRequest,
    ClienteResponse,
    ClienteUpdate,
)
from app.services import cliente_service

router = APIRouter(tags=["clientes"])


@router.post(
    "/clientes",
    response_model=ClienteResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_cliente(
    datos: ClienteCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ClienteResponse:
    """Registra un cliente nuevo para el usuario autenticado.

    Args:
        datos: Datos validados del cliente a crear.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ClienteResponse: El cliente creado.
    """
    return cliente_service.crear_cliente(db, usuario, datos)


@router.get("/clientes", response_model=list[ClienteResponse])
def listar_clientes(
    activo: bool | None = Query(default=None),
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[ClienteResponse]:
    """Lista los clientes del usuario, opcionalmente filtrados.

    Por defecto solo se devuelven los activos; para incluir los
    suspendidos hay que pedirlos con `activo=false`.

    Args:
        activo: Si es False incluye también los clientes suspendidos.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[ClienteResponse]: Clientes que cumplen el filtro.
    """
    solo_activos = activo is not False
    return cliente_service.listar_clientes(db, usuario, solo_activos=solo_activos)


@router.get("/clientes/{id_cliente}", response_model=ClienteResponse)
def obtener_cliente(
    id_cliente: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ClienteResponse:
    """Obtiene un cliente del usuario por su identificador.

    Args:
        id_cliente: Identificador del cliente a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ClienteResponse: El cliente encontrado.
    """
    return cliente_service.obtener_cliente(db, usuario, id_cliente)


@router.patch("/clientes/{id_cliente}", response_model=ClienteResponse)
def actualizar_cliente(
    id_cliente: int,
    datos: ClienteUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ClienteResponse:
    """Actualiza el nombre, el teléfono o la dirección de un cliente.

    Args:
        id_cliente: Identificador del cliente a actualizar.
        datos: Campos que se desean modificar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ClienteResponse: El cliente actualizado.
    """
    return cliente_service.actualizar_cliente(db, usuario, id_cliente, datos)


@router.post("/clientes/{id_cliente}/suspender", response_model=ClienteResponse)
def suspender_cliente(
    id_cliente: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ClienteResponse:
    """Suspende un cliente (reversible) para dejar de usarlo temporalmente.

    Args:
        id_cliente: Identificador del cliente a suspender.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ClienteResponse: El cliente suspendido.
    """
    return cliente_service.suspender_cliente(db, usuario, id_cliente)


@router.post("/clientes/{id_cliente}/activar", response_model=ClienteResponse)
def activar_cliente(
    id_cliente: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> ClienteResponse:
    """Reactiva un cliente previamente suspendido.

    Args:
        id_cliente: Identificador del cliente a activar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        ClienteResponse: El cliente reactivado.
    """
    return cliente_service.activar_cliente(db, usuario, id_cliente)


@router.post(
    "/clientes/{id_cliente}/eliminar",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_cliente(
    id_cliente: int,
    datos: ClienteEliminarRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Elimina de forma permanente un cliente tras confirmar la contraseña.

    Args:
        id_cliente: Identificador del cliente a eliminar.
        datos: Contraseña del usuario que confirma el borrado.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        None: Respuesta 204 sin contenido.
    """
    cliente_service.eliminar_cliente(db, usuario, id_cliente, datos.contrasena)

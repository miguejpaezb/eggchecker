from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.inventario import (
    AlertaInsumoResponse,
    InsumoCreate,
    InsumoResponse,
    InsumoUpdate,
    MovimientoCreate,
    MovimientoHistorialResponse,
    MovimientoResponse,
)
from app.services import insumo_service

router = APIRouter(tags=["insumos"])


@router.post(
    "/insumos",
    response_model=InsumoResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_insumo(
    datos: InsumoCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> InsumoResponse:
    """Registra un insumo nuevo para el usuario autenticado.

    Args:
        datos: Datos validados del insumo a crear.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        InsumoResponse: El insumo creado.
    """
    return insumo_service.crear_insumo(db, usuario, datos)


@router.get("/insumos", response_model=list[InsumoResponse])
def listar_insumos(
    categoria: int | None = Query(default=None),
    activo: bool | None = Query(default=None),
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[InsumoResponse]:
    """Lista los insumos del usuario, opcionalmente filtrados.

    Por defecto se excluyen los insumos desactivados; para incluir los
    inactivos hay que pedirlos con `activo=false`.

    Args:
        categoria: Categoría por la que filtrar los insumos.
        activo: Si es False incluye también los insumos desactivados.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[InsumoResponse]: Insumos que cumplen los filtros.
    """
    solo_activos = activo is not False
    return insumo_service.listar_insumos(
        db, usuario, categoria_id=categoria, solo_activos=solo_activos
    )


@router.get("/insumos/alertas", response_model=list[AlertaInsumoResponse])
def listar_alertas_insumos(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[AlertaInsumoResponse]:
    """Lista los insumos del usuario que están bajo su umbral mínimo.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[AlertaInsumoResponse]: Insumos en alerta con su déficit.
    """
    return insumo_service.listar_alertas(db, usuario)


@router.get("/insumos/{id_insumo}", response_model=InsumoResponse)
def obtener_insumo(
    id_insumo: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> InsumoResponse:
    """Obtiene un insumo del usuario por su identificador.

    Args:
        id_insumo: Identificador del insumo a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        InsumoResponse: El insumo encontrado.
    """
    return insumo_service.obtener_insumo(db, usuario, id_insumo)


@router.patch("/insumos/{id_insumo}", response_model=InsumoResponse)
def actualizar_insumo(
    id_insumo: int,
    datos: InsumoUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> InsumoResponse:
    """Actualiza los campos enviados de un insumo del usuario.

    Args:
        id_insumo: Identificador del insumo a actualizar.
        datos: Campos que se desean modificar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        InsumoResponse: El insumo actualizado.
    """
    return insumo_service.actualizar_insumo(db, usuario, id_insumo, datos)


@router.delete(
    "/insumos/{id_insumo}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_insumo(
    id_insumo: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Desactiva un insumo del usuario (eliminación lógica).

    Args:
        id_insumo: Identificador del insumo a desactivar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
    """
    insumo_service.eliminar_insumo(db, usuario, id_insumo)


@router.post(
    "/insumos/{id_insumo}/movimientos",
    response_model=MovimientoResponse,
    status_code=status.HTTP_201_CREATED,
)
def registrar_movimiento(
    id_insumo: int,
    datos: MovimientoCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> MovimientoResponse:
    """Registra una entrada o salida y devuelve el stock resultante.

    Args:
        id_insumo: Identificador del insumo afectado.
        datos: Tipo de movimiento, cantidad y observaciones.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        MovimientoResponse: El movimiento creado con el stock resultante.
    """
    movimiento, stock_resultante = insumo_service.registrar_movimiento(
        db, usuario, id_insumo, datos
    )
    return MovimientoResponse(
        id_movimiento=movimiento.id_movimiento,
        id_insumo=movimiento.id_insumo,
        tipo_movimiento=movimiento.tipo_movimiento,
        cantidad=movimiento.cantidad,
        fecha_movimiento=movimiento.fecha_movimiento,
        observaciones=movimiento.observaciones,
        stock_resultante=stock_resultante,
    )


@router.get(
    "/insumos/{id_insumo}/movimientos",
    response_model=list[MovimientoHistorialResponse],
)
def listar_movimientos(
    id_insumo: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[MovimientoHistorialResponse]:
    """Lista el historial de movimientos de un insumo del usuario.

    Args:
        id_insumo: Identificador del insumo consultado.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[MovimientoHistorialResponse]: Movimientos recientes primero.
    """
    return insumo_service.listar_movimientos(db, usuario, id_insumo)

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.pedido import Pedido
from app.models.usuario import Usuario
from app.schemas.ventas import (
    CambiarEstadoRequest,
    PedidoCreate,
    PedidoDetalleResponse,
    PedidoEliminarRequest,
    PedidoResponse,
    PedidoUpdate,
    StockResponse,
)
from app.services import stock_service, venta_service

router = APIRouter(tags=["ventas"])


def _con_detalle(pedido: Pedido) -> PedidoResponse:
    """Arma la respuesta de un pedido con cliente y detalle calculado."""
    detalles = [
        PedidoDetalleResponse(
            id_detalle_pedido=detalle.id_detalle_pedido,
            id_tipo=detalle.id_tipo,
            nombre_tipo=detalle.tipo.nombre_tipo,
            cantidad=detalle.cantidad,
            precio_unitario=detalle.precio_unitario,
            subtotal=detalle.precio_unitario * detalle.cantidad,
        )
        for detalle in pedido.detalles
    ]
    return PedidoResponse(
        id_pedido=pedido.id_pedido,
        id_cliente=pedido.id_cliente,
        id_usuario=pedido.id_usuario,
        cliente_nombre=pedido.cliente.nombre_cliente,
        cliente_direccion=pedido.cliente.direccion,
        fecha_pedido=pedido.fecha_pedido,
        estado_pedido=pedido.estado_pedido,
        valor_total=pedido.valor_total,
        unidades_totales=sum(detalle.cantidad for detalle in pedido.detalles),
        detalles=detalles,
    )


@router.get("/ventas/stock", response_model=StockResponse)
def obtener_stock(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> StockResponse:
    """Devuelve el stock de huevos del usuario por tipo de huevo.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        StockResponse: Total disponible y detalle por tipo.
    """
    return stock_service.obtener_stock(db, usuario)


@router.get("/ventas/pedidos", response_model=list[PedidoResponse])
def listar_pedidos(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[PedidoResponse]:
    """Lista los pedidos del usuario autenticado.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[PedidoResponse]: Pedidos con su cliente y detalle.
    """
    return [
        _con_detalle(pedido) for pedido in venta_service.listar_pedidos(db, usuario)
    ]


@router.get("/ventas/pedidos/{id_pedido}", response_model=PedidoResponse)
def obtener_pedido(
    id_pedido: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PedidoResponse:
    """Obtiene un pedido del usuario por su identificador.

    Args:
        id_pedido: Identificador del pedido a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PedidoResponse: El pedido encontrado con su detalle.
    """
    return _con_detalle(venta_service.obtener_pedido(db, usuario, id_pedido))


@router.post(
    "/ventas/pedidos",
    response_model=PedidoResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_pedido(
    datos: PedidoCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PedidoResponse:
    """Registra un pedido nuevo para el usuario autenticado.

    Args:
        datos: Cliente y líneas del pedido a crear.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PedidoResponse: El pedido creado con su detalle.
    """
    return _con_detalle(venta_service.crear_pedido(db, usuario, datos))


@router.patch("/ventas/pedidos/{id_pedido}", response_model=PedidoResponse)
def actualizar_pedido(
    id_pedido: int,
    datos: PedidoUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PedidoResponse:
    """Edita un pedido pendiente reajustando el stock.

    Args:
        id_pedido: Identificador del pedido a actualizar.
        datos: Cliente y/o líneas nuevas del pedido.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PedidoResponse: El pedido actualizado con su detalle.
    """
    return _con_detalle(venta_service.actualizar_pedido(db, usuario, id_pedido, datos))


@router.patch("/ventas/pedidos/{id_pedido}/estado", response_model=PedidoResponse)
def cambiar_estado(
    id_pedido: int,
    datos: CambiarEstadoRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PedidoResponse:
    """Avanza el estado de un pedido a 'enviado' o 'recibido'.

    Args:
        id_pedido: Identificador del pedido a mover.
        datos: Estado destino del pedido.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PedidoResponse: El pedido con el estado actualizado.
    """
    return _con_detalle(
        venta_service.cambiar_estado(db, usuario, id_pedido, datos.estado)
    )


@router.post("/ventas/pedidos/{id_pedido}/cancelar", response_model=PedidoResponse)
def cancelar_pedido(
    id_pedido: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> PedidoResponse:
    """Cancela un pedido pendiente o en camino y repone el stock.

    Args:
        id_pedido: Identificador del pedido a cancelar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        PedidoResponse: El pedido con estado 'cancelado'.
    """
    return _con_detalle(venta_service.cancelar_pedido(db, usuario, id_pedido))


@router.post(
    "/ventas/pedidos/{id_pedido}/eliminar",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_pedido(
    id_pedido: int,
    datos: PedidoEliminarRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Elimina un pedido tras confirmar la contraseña y repone el stock.

    Args:
        id_pedido: Identificador del pedido a eliminar.
        datos: Contraseña del usuario que confirma el borrado.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        None: Respuesta 204 sin contenido.
    """
    venta_service.eliminar_pedido(db, usuario, id_pedido, datos.contrasena)

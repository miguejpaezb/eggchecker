from datetime import date
from decimal import Decimal

from app.core.security import verificar_hash
from app.models.cliente import Cliente
from app.models.detalle_pedido import DetallePedido
from app.models.pedido import Pedido
from app.models.stock_produccion import StockProduccion
from app.models.tipo_huevo import TipoHuevo
from app.models.usuario import Usuario
from app.schemas.ventas import (
    PedidoCreate,
    PedidoDetalleCreate,
    PedidoUpdate,
)
from app.services import stock_service
from fastapi import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

# Estados en los que un pedido aún se puede editar, cancelar o eliminar.
_ESTADOS_OPERABLES = ("pendiente", "enviado")


def crear_pedido(db: Session, usuario: Usuario, datos: PedidoCreate) -> Pedido:
    """Registra un pedido pendiente y descuenta el stock de cada tipo.

    Valida que el cliente sea del usuario y esté activo, que cada línea
    tenga cantidad entera disponible en stock y guarda el total calculado
    como `cantidad * precio_unitario`. Fija `fecha_pedido` con la fecha
    del servidor y actualiza la última compra del cliente.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        datos: Cliente y líneas del pedido a crear.

    Returns:
        Pedido: El pedido recién creado con su detalle.

    Raises:
        HTTPException: 404 si el cliente o un tipo no existen; 400 si el
            cliente está suspendido, hay un tipo repetido o no hay stock.
    """
    cliente = _obtener_cliente(db, usuario, datos.id_cliente)
    detalles = _normalizar_detalles(datos.detalles)
    nombres = _mapa_nombres_tipos(db, detalles)
    registros = _mapa_stock(db, usuario.id_usuario)

    _validar_stock(registros, detalles, nombres, reservado={})

    pedido = Pedido(
        id_cliente=cliente.id_cliente,
        id_usuario=usuario.id_usuario,
        fecha_pedido=date.today(),
        estado_pedido="pendiente",
        valor_total=Decimal("0.00"),
    )
    total = Decimal("0.00")
    for id_tipo, linea in detalles.items():
        stock_service.aplicar_delta(
            db,
            usuario.id_usuario,
            id_tipo,
            -linea.cantidad,
            valor_unidad=linea.precio_unitario,
        )
        total += linea.precio_unitario * linea.cantidad
        pedido.detalles.append(
            DetallePedido(
                id_tipo=id_tipo,
                cantidad=linea.cantidad,
                precio_unitario=linea.precio_unitario,
            )
        )

    pedido.valor_total = total
    cliente.fecha_ultima_compra = date.today()
    db.add(pedido)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(
            status_code=409, detail="No se pudo registrar el pedido"
        ) from exc
    db.refresh(pedido)
    return pedido


def listar_pedidos(db: Session, usuario: Usuario) -> list[Pedido]:
    """Lista los pedidos del usuario, de los más recientes a los más antiguos.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los pedidos.

    Returns:
        list[Pedido]: Pedidos del usuario con su detalle.
    """
    return (
        db.query(Pedido)
        .filter(Pedido.id_usuario == usuario.id_usuario)
        .order_by(Pedido.fecha_pedido.desc(), Pedido.id_pedido.desc())
        .all()
    )


def obtener_pedido(db: Session, usuario: Usuario, id_pedido: int) -> Pedido:
    """Obtiene un pedido del usuario por su identificador.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        id_pedido: Identificador del pedido a consultar.

    Returns:
        Pedido: El pedido encontrado.

    Raises:
        HTTPException: 404 si el pedido no existe o no es del usuario.
    """
    return _obtener_pedido_de_usuario(db, usuario, id_pedido)


def actualizar_pedido(
    db: Session, usuario: Usuario, id_pedido: int, datos: PedidoUpdate
) -> Pedido:
    """Edita un pedido pendiente reajustando el stock por diferencia.

    Solo se pueden editar pedidos en estado 'pendiente'. Si cambian las
    líneas, primero se libera lo reservado y luego se valida el nuevo
    consumo contra el stock disponible.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        id_pedido: Identificador del pedido a actualizar.
        datos: Cliente y/o líneas nuevas del pedido.

    Returns:
        Pedido: El pedido con los cambios aplicados.

    Raises:
        HTTPException: 404 si el pedido, el cliente o un tipo no existen;
            400 si el pedido no está pendiente, el cliente está suspendido,
            hay un tipo repetido o no hay stock suficiente.
    """
    pedido = _obtener_pedido_de_usuario(db, usuario, id_pedido)
    if pedido.estado_pedido != "pendiente":
        raise HTTPException(
            status_code=400,
            detail="Solo se pueden editar pedidos pendientes",
        )

    if datos.id_cliente is not None:
        cliente = _obtener_cliente(db, usuario, datos.id_cliente)
        pedido.id_cliente = cliente.id_cliente

    if datos.detalles is not None:
        _reajustar_detalles(db, usuario, pedido, datos.detalles)

    db.commit()
    db.refresh(pedido)
    return pedido


def cambiar_estado(
    db: Session, usuario: Usuario, id_pedido: int, estado: str
) -> Pedido:
    """Avanza el estado de un pedido a 'enviado' o 'recibido'.

    Transiciones permitidas: pendiente→enviado, pendiente→recibido y
    enviado→recibido. El estado no afecta el stock.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        id_pedido: Identificador del pedido a mover.
        estado: Estado destino ('enviado' o 'recibido').

    Returns:
        Pedido: El pedido con el estado actualizado.

    Raises:
        HTTPException: 404 si el pedido no es del usuario; 400 si la
            transición no es válida.
    """
    pedido = _obtener_pedido_de_usuario(db, usuario, id_pedido)
    transiciones = {
        "enviado": ("pendiente",),
        "recibido": ("pendiente", "enviado"),
    }
    if pedido.estado_pedido not in transiciones.get(estado, ()):
        raise HTTPException(
            status_code=400,
            detail=f"No se puede pasar el pedido a '{estado}'",
        )
    pedido.estado_pedido = estado
    db.commit()
    db.refresh(pedido)
    return pedido


def cancelar_pedido(db: Session, usuario: Usuario, id_pedido: int) -> Pedido:
    """Cancela un pedido pendiente o en camino y repone el stock.

    La cancelación no borra el pedido: queda en los registros con estado
    'cancelado'.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        id_pedido: Identificador del pedido a cancelar.

    Returns:
        Pedido: El pedido con estado 'cancelado'.

    Raises:
        HTTPException: 404 si el pedido no es del usuario; 400 si no está
            pendiente o en camino.
    """
    pedido = _obtener_pedido_de_usuario(db, usuario, id_pedido)
    if pedido.estado_pedido not in _ESTADOS_OPERABLES:
        raise HTTPException(
            status_code=400,
            detail="Solo se pueden cancelar pedidos pendientes o en camino",
        )
    _reponer_stock(db, usuario, pedido)
    pedido.estado_pedido = "cancelado"
    db.commit()
    db.refresh(pedido)
    return pedido


def eliminar_pedido(
    db: Session, usuario: Usuario, id_pedido: int, contrasena: str
) -> None:
    """Borra un pedido tras confirmar la contraseña y repone el stock.

    Solo se pueden eliminar pedidos pendientes o en camino; un pedido
    entregado o ya cancelado no se elimina.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del pedido.
        id_pedido: Identificador del pedido a eliminar.
        contrasena: Contraseña del usuario para confirmar el borrado.

    Raises:
        HTTPException: 404 si el pedido no es del usuario; 401 si la
            contraseña no coincide; 400 si el estado no permite eliminarlo.
    """
    pedido = _obtener_pedido_de_usuario(db, usuario, id_pedido)
    if not verificar_hash(contrasena, usuario.contrasena_hash):
        raise HTTPException(status_code=401, detail="Contraseña incorrecta")
    if pedido.estado_pedido not in _ESTADOS_OPERABLES:
        raise HTTPException(
            status_code=400,
            detail="No se puede eliminar un pedido en este estado",
        )
    _reponer_stock(db, usuario, pedido)
    db.delete(pedido)
    db.commit()


def _reajustar_detalles(
    db: Session,
    usuario: Usuario,
    pedido: Pedido,
    detalles: list[PedidoDetalleCreate],
) -> None:
    """Reemplaza las líneas de un pedido y ajusta el stock por diferencia."""
    nuevos = _normalizar_detalles(detalles)
    nombres = _mapa_nombres_tipos(db, nuevos)
    viejos = {detalle.id_tipo: detalle.cantidad for detalle in pedido.detalles}
    registros = _mapa_stock(db, usuario.id_usuario)
    _validar_stock(registros, nuevos, nombres, reservado=viejos)

    pedido.detalles.clear()
    db.flush()

    total = Decimal("0.00")
    for id_tipo in set(viejos) | set(nuevos):
        anterior = viejos.get(id_tipo, 0)
        linea = nuevos.get(id_tipo)
        nueva = linea.cantidad if linea else 0
        stock_service.aplicar_delta(
            db,
            usuario.id_usuario,
            id_tipo,
            anterior - nueva,
            valor_unidad=linea.precio_unitario if linea else None,
        )

    for id_tipo, linea in nuevos.items():
        pedido.detalles.append(
            DetallePedido(
                id_tipo=id_tipo,
                cantidad=linea.cantidad,
                precio_unitario=linea.precio_unitario,
            )
        )
        total += linea.precio_unitario * linea.cantidad
    pedido.valor_total = total


def _reponer_stock(db: Session, usuario: Usuario, pedido: Pedido) -> None:
    """Devuelve al stock los huevos reservados por un pedido."""
    for detalle in pedido.detalles:
        stock_service.aplicar_delta(
            db, usuario.id_usuario, detalle.id_tipo, detalle.cantidad
        )


def _validar_stock(
    registros: dict[int, StockProduccion],
    detalles: dict[int, PedidoDetalleCreate],
    nombres: dict[int, str],
    reservado: dict[int, int],
) -> None:
    """Verifica que cada línea tenga stock suficiente (más lo reservado)."""
    for id_tipo, linea in detalles.items():
        registro = registros.get(id_tipo)
        disponible = (registro.cantidad_actual if registro else 0) + reservado.get(
            id_tipo, 0
        )
        if linea.cantidad > disponible:
            raise HTTPException(
                status_code=400,
                detail=(
                    f"Stock insuficiente de {nombres[id_tipo]}: "
                    f"disponible {disponible}"
                ),
            )


def _normalizar_detalles(
    detalles: list[PedidoDetalleCreate],
) -> dict[int, PedidoDetalleCreate]:
    """Indexa las líneas por tipo y rechaza tipos repetidos."""
    indexados: dict[int, PedidoDetalleCreate] = {}
    for linea in detalles:
        if linea.id_tipo in indexados:
            raise HTTPException(
                status_code=400,
                detail="No se puede repetir el mismo tipo de huevo en un pedido",
            )
        indexados[linea.id_tipo] = linea
    return indexados


def _mapa_nombres_tipos(
    db: Session, detalles: dict[int, PedidoDetalleCreate]
) -> dict[int, str]:
    """Devuelve el nombre de cada tipo de huevo o 404 si alguno no existe."""
    tipos = db.query(TipoHuevo).filter(TipoHuevo.id_tipo.in_(detalles.keys())).all()
    nombres = {tipo.id_tipo: tipo.nombre_tipo for tipo in tipos}
    if len(nombres) != len(detalles):
        raise HTTPException(status_code=404, detail="Tipo de huevo no encontrado")
    if stock_service.TIPO_HUEVO_NO_VENDIBLE in nombres.values():
        raise HTTPException(
            status_code=400,
            detail="Los huevos No_apto no se pueden vender",
        )
    return nombres


def _mapa_stock(db: Session, id_usuario: int) -> dict[int, StockProduccion]:
    """Devuelve el stock del usuario indexado por id_tipo."""
    filas = (
        db.query(StockProduccion).filter(StockProduccion.id_usuario == id_usuario).all()
    )
    return {fila.id_tipo: fila for fila in filas}


def _obtener_cliente(db: Session, usuario: Usuario, id_cliente: int) -> Cliente:
    """Busca un cliente activo del usuario o lanza 404/400."""
    cliente = (
        db.query(Cliente)
        .filter(
            Cliente.id_cliente == id_cliente,
            Cliente.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if cliente is None:
        raise HTTPException(status_code=404, detail="Cliente no encontrado")
    if not cliente.activo:
        raise HTTPException(
            status_code=400,
            detail="El cliente está suspendido; actívalo para registrar la venta",
        )
    return cliente


def _obtener_pedido_de_usuario(db: Session, usuario: Usuario, id_pedido: int) -> Pedido:
    """Busca un pedido que pertenezca al usuario o lanza 404."""
    pedido = (
        db.query(Pedido)
        .filter(
            Pedido.id_pedido == id_pedido,
            Pedido.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if pedido is None:
        raise HTTPException(status_code=404, detail="Pedido no encontrado")
    return pedido

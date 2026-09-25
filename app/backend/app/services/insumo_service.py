from decimal import Decimal

from app.models.categoria_insumo import CategoriaInsumo
from app.models.insumo import Insumo
from app.models.movimiento_insumo import MovimientoInsumo
from app.models.usuario import Usuario
from app.schemas.inventario import (
    AlertaInsumoResponse,
    InsumoCreate,
    InsumoUpdate,
    MovimientoCreate,
)
from app.services import notificacion_service
from fastapi import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session


def crear_insumo(db: Session, usuario: Usuario, datos: InsumoCreate) -> Insumo:
    """Registra un insumo nuevo propiedad del usuario autenticado.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        datos: Datos validados del insumo a crear.

    Returns:
        Insumo: El insumo recién creado.

    Raises:
        HTTPException: 404 si la categoría indicada no existe.
    """
    _validar_categoria_existe(db, datos.id_categoria)
    insumo = Insumo(
        id_usuario=usuario.id_usuario,
        id_categoria=datos.id_categoria,
        nombre_insumo=datos.nombre_insumo,
        unidad_medida=datos.unidad_medida,
        stock_actual=datos.stock_actual,
        umbral_minimo=datos.umbral_minimo,
    )
    db.add(insumo)
    db.commit()
    db.refresh(insumo)
    notificacion_service.sincronizar_aviso_stock(db, insumo)
    return insumo


def listar_insumos(
    db: Session,
    usuario: Usuario,
    categoria_id: int | None = None,
    solo_activos: bool = True,
) -> list[Insumo]:
    """Lista los insumos del usuario, opcionalmente filtrados.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los insumos.
        categoria_id: Categoría por la que filtrar los insumos.
        solo_activos: Si True, excluye los insumos desactivados.

    Returns:
        list[Insumo]: Insumos del usuario que cumplen los filtros.
    """
    consulta = db.query(Insumo).filter(
        Insumo.id_usuario == usuario.id_usuario,
        Insumo.descontinuado.is_(False),
    )
    if categoria_id is not None:
        consulta = consulta.filter(Insumo.id_categoria == categoria_id)
    if solo_activos:
        consulta = consulta.filter(Insumo.activo.is_(True))
    return consulta.order_by(Insumo.nombre_insumo).all()


def obtener_insumo(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Obtiene un insumo del usuario por su identificador.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo a consultar.

    Returns:
        Insumo: El insumo encontrado.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario.
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    return insumo


def actualizar_insumo(
    db: Session, usuario: Usuario, id_insumo: int, datos: InsumoUpdate
) -> Insumo:
    """Aplica los cambios enviados a un insumo del usuario.

    Solo se editan nombre, unidad de medida y umbral mínimo; el insumo no
    puede estar suspendido ni descontinuado.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo a actualizar.
        datos: Campos que se desean modificar.

    Returns:
        Insumo: El insumo con los cambios aplicados.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario;
            400 si está suspendido o descontinuado.
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    _validar_insumo_operable(insumo)
    campos = datos.model_dump(exclude_unset=True)
    for campo, valor in campos.items():
        if valor is not None:
            setattr(insumo, campo, valor)
    db.commit()
    db.refresh(insumo)
    if "umbral_minimo" in campos:
        notificacion_service.sincronizar_aviso_stock(db, insumo)
    return insumo


def suspender_insumo(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Suspende un insumo (activo = False) de forma reversible.

    Un insumo suspendido no se puede usar ni manipular hasta reactivarlo.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo a suspender.

    Returns:
        Insumo: El insumo con activo en False.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario;
            400 si ya está descontinuado.
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    if insumo.descontinuado:
        raise HTTPException(
            status_code=400,
            detail="No se puede suspender un insumo descontinuado",
        )
    insumo.activo = False
    db.commit()
    db.refresh(insumo)
    return insumo


def activar_insumo(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Reactiva un insumo suspendido.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo a activar.

    Returns:
        Insumo: El insumo con activo en True.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario;
            400 si está descontinuado (no se puede reactivar).
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    if insumo.descontinuado:
        raise HTTPException(
            status_code=400,
            detail="Un insumo descontinuado no se puede reactivar",
        )
    insumo.activo = True
    db.commit()
    db.refresh(insumo)
    notificacion_service.sincronizar_aviso_stock(db, insumo)
    return insumo


def descontinuar_insumo(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Descontinúa un insumo de forma permanente.

    El insumo deja de listarse y no puede usarse ni reactivarse, pero sus
    movimientos históricos se conservan.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo a descontinuar.

    Returns:
        Insumo: El insumo con descontinuado en True.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario;
            400 si ya estaba descontinuado.
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    if insumo.descontinuado:
        raise HTTPException(
            status_code=400,
            detail="El insumo ya está descontinuado",
        )
    insumo.descontinuado = True
    insumo.activo = False
    db.commit()
    db.refresh(insumo)
    notificacion_service.descartar_avisos_stock(db, insumo)
    return insumo


def registrar_movimiento(
    db: Session,
    usuario: Usuario,
    id_insumo: int,
    datos: MovimientoCreate,
) -> tuple[MovimientoInsumo, Decimal]:
    """Registra una entrada o salida y actualiza el stock en una transacción.

    Replica la lógica del `sp_mover_insumo` del DDL original: valida el
    stock antes de una salida, inserta el movimiento y actualiza
    `stock_actual` en la misma operación. Si algo falla se hace rollback
    para que nunca quede un movimiento sin su ajuste de stock (o a la
    inversa).

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo afectado.
        datos: Tipo de movimiento, cantidad y observaciones.

    Returns:
        tuple[MovimientoInsumo, Decimal]: El movimiento recién creado y el
        stock resultante del insumo.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario;
            400 si el insumo está suspendido/descontinuado o si una salida
            supera el stock actual.
    """
    insumo = _obtener_insumo_de_usuario(db, usuario, id_insumo)
    _validar_insumo_operable(insumo)
    if datos.tipo_movimiento == "salida" and insumo.stock_actual < datos.cantidad:
        raise HTTPException(
            status_code=400,
            detail="Stock insuficiente para registrar la salida",
        )

    if datos.tipo_movimiento == "entrada":
        stock_resultante = insumo.stock_actual + datos.cantidad
    else:
        stock_resultante = insumo.stock_actual - datos.cantidad

    movimiento = MovimientoInsumo(
        id_insumo=id_insumo,
        tipo_movimiento=datos.tipo_movimiento,
        cantidad=datos.cantidad,
        observaciones=datos.observaciones,
    )
    insumo.stock_actual = stock_resultante
    db.add(movimiento)
    try:
        db.commit()
    except IntegrityError as exc:
        # Si el constraint chk_insumo_stock u otro falla, no debe quedar
        # el movimiento registrado sin su ajuste correspondiente.
        db.rollback()
        raise HTTPException(
            status_code=400,
            detail="No se pudo registrar el movimiento: stock inconsistente",
        ) from exc
    db.refresh(movimiento)
    notificacion_service.sincronizar_aviso_stock(db, insumo)
    return movimiento, stock_resultante


def listar_movimientos(
    db: Session, usuario: Usuario, id_insumo: int
) -> list[MovimientoInsumo]:
    """Lista el historial de movimientos de un insumo del usuario.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del insumo.
        id_insumo: Identificador del insumo del que se consulta el historial.

    Returns:
        list[MovimientoInsumo]: Movimientos ordenados del más reciente al
        más antiguo.

    Raises:
        HTTPException: 404 si el insumo no existe o no es del usuario.
    """
    _obtener_insumo_de_usuario(db, usuario, id_insumo)
    return (
        db.query(MovimientoInsumo)
        .filter(MovimientoInsumo.id_insumo == id_insumo)
        .order_by(
            MovimientoInsumo.fecha_movimiento.desc(),
            MovimientoInsumo.id_movimiento.desc(),
        )
        .all()
    )


def listar_alertas(db: Session, usuario: Usuario) -> list[AlertaInsumoResponse]:
    """Lista los insumos del usuario que están bajo su umbral mínimo.

    Replica la vista `v_alertas_insumo` del DDL original acotada al
    usuario: insumos activos con `stock_actual < umbral_minimo`, con su
    categoría y el déficit (`umbral_minimo - stock_actual`), ordenados de
    mayor a menor déficit.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los insumos.

    Returns:
        list[AlertaInsumoResponse]: Insumos en alerta con su déficit.
    """
    filas = (
        db.query(Insumo, CategoriaInsumo.nombre_categ)
        .join(
            CategoriaInsumo,
            Insumo.id_categoria == CategoriaInsumo.id_categoria,
        )
        .filter(
            Insumo.id_usuario == usuario.id_usuario,
            Insumo.activo.is_(True),
            Insumo.descontinuado.is_(False),
            Insumo.stock_actual < Insumo.umbral_minimo,
        )
        .order_by((Insumo.umbral_minimo - Insumo.stock_actual).desc())
        .all()
    )
    return [
        AlertaInsumoResponse(
            id_insumo=insumo.id_insumo,
            categoria=categoria,
            nombre_insumo=insumo.nombre_insumo,
            stock_actual=insumo.stock_actual,
            umbral_minimo=insumo.umbral_minimo,
            deficit=insumo.umbral_minimo - insumo.stock_actual,
        )
        for insumo, categoria in filas
    ]


def _obtener_insumo_de_usuario(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Busca un insumo que pertenezca al usuario o lanza 404."""
    insumo = (
        db.query(Insumo)
        .filter(
            Insumo.id_insumo == id_insumo,
            Insumo.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if insumo is None:
        raise HTTPException(status_code=404, detail="Insumo no encontrado")
    return insumo


def _validar_insumo_operable(insumo: Insumo) -> None:
    """Impide operar sobre insumos suspendidos o descontinuados."""
    if insumo.descontinuado:
        raise HTTPException(
            status_code=400,
            detail="El insumo está descontinuado y no se puede manipular",
        )
    if not insumo.activo:
        raise HTTPException(
            status_code=400,
            detail="El insumo está suspendido; actívalo para poder usarlo",
        )


def _validar_categoria_existe(db: Session, id_categoria: int) -> None:
    """Comprueba que la categoría exista en el catálogo o lanza 404."""
    categoria = db.get(CategoriaInsumo, id_categoria)
    if categoria is None:
        raise HTTPException(status_code=404, detail="Categoría no encontrada")

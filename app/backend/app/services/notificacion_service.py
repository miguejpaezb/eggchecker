from datetime import date, datetime, timedelta
from decimal import Decimal

from app.models.camada import (
    EDAD_DECISION_SEMANAS,
    EDAD_PRODUCCION_SEMANAS,
    Camada,
)
from app.models.evento_sanitario import EventoSanitario
from app.models.insumo import Insumo
from app.models.notificacion import (
    TIPO_CAMADA_LIMITE,
    TIPO_PRODUCCION_BAJA,
    TIPO_PRODUCCION_SIN_REGISTRO,
    TIPO_SIN_STOCK,
    TIPO_STOCK_BAJO,
    TIPO_STOCK_MINIMO,
    TIPOS_STOCK,
    Notificacion,
)
from app.models.produccion_diaria import ProduccionDiaria
from app.models.usuario import Usuario
from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

# Postura mínima esperada: por debajo del 85% de las aves activas se avisa.
UMBRAL_POSTURA = Decimal("0.85")


def listar_notificaciones(db: Session, usuario: Usuario) -> list[Notificacion]:
    """Lista las notificaciones del usuario, sin leídas primero.

    Antes de listar, genera las notificaciones de camadas que superaron
    la vida productiva, los avisos de stock y el control diario de la
    recolección de ayer.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las notificaciones.

    Returns:
        list[Notificacion]: Notificaciones ordenadas (no leídas primero).
    """
    _generar_para_camadas_limite(db, usuario)
    _generar_para_insumos_stock(db, usuario)
    _generar_para_produccion(db, usuario)
    return (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
        )
        .order_by(Notificacion.leida.asc(), Notificacion.fecha_creacion.desc())
        .all()
    )


def marcar_leida(db: Session, usuario: Usuario, id_notificacion: int) -> Notificacion:
    """Marca una notificación del usuario como leída.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la notificación.
        id_notificacion: Identificador de la notificación.

    Returns:
        Notificacion: La notificación ya marcada como leída.

    Raises:
        HTTPException: 404 si la notificación no es del usuario.
    """
    notificacion = _obtener_notificacion(db, usuario, id_notificacion)
    notificacion.leida = 1
    db.commit()
    db.refresh(notificacion)
    return notificacion


def marcar_todas_leidas(db: Session, usuario: Usuario) -> int:
    """Marca como leídas todas las notificaciones del usuario.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las notificaciones.

    Returns:
        int: Cantidad de notificaciones actualizadas.
    """
    _generar_para_camadas_limite(db, usuario)
    _generar_para_insumos_stock(db, usuario)
    _generar_para_produccion(db, usuario)
    total = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
            Notificacion.leida == 0,
        )
        .update({Notificacion.leida: 1})
    )
    db.commit()
    return int(total)


def eliminar_notificacion(db: Session, usuario: Usuario, id_notificacion: int) -> None:
    """Elimina (oculta) una notificación del usuario.

    Se marca como eliminada en lugar de borrarla para que la generación
    perezosa no la vuelva a crear.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la notificación.
        id_notificacion: Identificador de la notificación a eliminar.

    Raises:
        HTTPException: 404 si la notificación no es del usuario.
    """
    notificacion = _obtener_notificacion(db, usuario, id_notificacion)
    notificacion.eliminada = 1
    db.commit()


def _generar_para_camadas_limite(db: Session, usuario: Usuario) -> None:
    """Crea el aviso de una camada que superó las 72 semanas.

    Genera una única notificación por camada y tipo; si ya existe, no la
    vuelve a crear. Se ejecuta de forma perezosa al listar.
    """
    camadas = (
        db.query(Camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            Camada.estado == "activa",
            Camada.edad_semanas >= EDAD_DECISION_SEMANAS,
        )
        .all()
    )
    if not camadas:
        return

    existentes = {
        fila[0]
        for fila in db.query(Notificacion.id_camada)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.tipo == TIPO_CAMADA_LIMITE,
        )
        .all()
    }

    creada = False
    for camada in camadas:
        if camada.id_camada in existentes:
            continue
        db.add(
            Notificacion(
                id_usuario=usuario.id_usuario,
                id_camada=camada.id_camada,
                tipo=TIPO_CAMADA_LIMITE,
                titulo="Camada al límite",
                mensaje=(f"Camada {camada.nombre_camada} ha pasado las 72 semanas"),
            )
        )
        creada = True

    if creada:
        db.commit()


def sincronizar_aviso_stock(db: Session, insumo: Insumo) -> None:
    """Crea, actualiza o cierra el aviso de stock de un insumo.

    Mantiene un único aviso por insumo según su nivel de stock. Un aviso
    más severo reemplaza al anterior (`stock_minimo` → `stock_bajo` →
    `sin_stock`). Si el usuario lo eliminó y el stock no cambió, no lo
    vuelve a crear; al volver a un nivel óptimo, el aviso se cierra.

    Args:
        db: Sesión de base de datos.
        insumo: Insumo cuyo aviso de stock se sincroniza.
    """
    aviso = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == insumo.id_usuario,
            Notificacion.id_insumo == insumo.id_insumo,
            Notificacion.tipo.in_(TIPOS_STOCK),
        )
        .first()
    )

    if insumo.descontinuado or not insumo.activo or insumo.umbral_minimo <= 0:
        _cerrar_aviso(db, aviso)
        return

    tipo = _tipo_aviso_stock(insumo)
    if tipo is None:
        _cerrar_aviso(db, aviso)
        return

    titulo, mensaje = _texto_aviso_stock(insumo, tipo)

    if aviso is None:
        db.add(
            Notificacion(
                id_usuario=insumo.id_usuario,
                id_insumo=insumo.id_insumo,
                tipo=tipo,
                titulo=titulo,
                mensaje=mensaje,
                stock_referencia=insumo.stock_actual,
            )
        )
        db.commit()
        return

    if aviso.eliminada and aviso.stock_referencia == insumo.stock_actual:
        # El usuario descartó el aviso para este mismo stock.
        return

    cambio_severidad = aviso.tipo != tipo
    aviso.tipo = tipo
    aviso.titulo = titulo
    aviso.mensaje = mensaje
    aviso.stock_referencia = insumo.stock_actual
    aviso.eliminada = 0
    if cambio_severidad:
        aviso.leida = 0
    db.commit()


def descartar_avisos_stock(db: Session, insumo: Insumo) -> None:
    """Cierra los avisos de stock de un insumo (ej. al descontinuarlo)."""
    avisos = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == insumo.id_usuario,
            Notificacion.id_insumo == insumo.id_insumo,
            Notificacion.tipo.in_(TIPOS_STOCK),
            Notificacion.eliminada == 0,
        )
        .all()
    )
    for aviso in avisos:
        aviso.eliminada = 1
    if avisos:
        db.commit()


def _generar_para_insumos_stock(db: Session, usuario: Usuario) -> None:
    """Sincroniza los avisos de stock de todos los insumos del usuario."""
    insumos = (
        db.query(Insumo)
        .filter(
            Insumo.id_usuario == usuario.id_usuario,
            Insumo.descontinuado.is_(False),
        )
        .all()
    )
    for insumo in insumos:
        sincronizar_aviso_stock(db, insumo)


def _generar_para_produccion(db: Session, usuario: Usuario) -> None:
    """Revisa la recolección de ayer y avisa si la postura fue baja.

    Solo corre a partir de la hora configurada por el usuario (por defecto
    05:00) y evalúa las camadas activas que ya entraron en producción. Un
    solo aviso por camada y por día evaluado.
    """
    if not usuario.notif_produccion_baja:
        return
    ahora = datetime.now()
    if ahora.time() < usuario.notif_produccion_hora:
        return
    ayer = ahora.date() - timedelta(days=1)
    camadas = (
        db.query(Camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            Camada.estado == "activa",
            Camada.edad_semanas >= EDAD_PRODUCCION_SEMANAS,
            Camada.fecha_ingreso <= ayer,
        )
        .all()
    )
    for camada in camadas:
        _avisar_produccion(db, usuario, camada, ayer)


def _avisar_produccion(
    db: Session, usuario: Usuario, camada: Camada, fecha: date
) -> None:
    """Crea el aviso de producción de una camada para la fecha evaluada.

    Distingue dos casos: no hay registro de recolección (no se puede
    calcular la postura) o el registro existe y quedó por debajo del
    umbral. Si el aviso de esa camada y fecha ya existe, no se repite.
    """
    if _existe_aviso_produccion(db, usuario, camada, fecha):
        return
    aves = _aves_en_fecha(db, camada, fecha)
    if aves <= 0:
        return
    produccion = (
        db.query(ProduccionDiaria)
        .filter(
            ProduccionDiaria.id_camada == camada.id_camada,
            ProduccionDiaria.fecha_recoleccion == fecha,
        )
        .first()
    )
    if produccion is None:
        aviso = Notificacion(
            id_usuario=usuario.id_usuario,
            id_camada=camada.id_camada,
            tipo=TIPO_PRODUCCION_SIN_REGISTRO,
            titulo="Falta el registro de producción",
            mensaje=(
                f"No registraste la recolección de {camada.nombre_camada} "
                f"del {fecha:%d/%m/%Y}."
            ),
            fecha_referencia=fecha,
        )
    else:
        tasa = Decimal(produccion.total_huevos) / Decimal(aves)
        if tasa >= UMBRAL_POSTURA:
            return
        aviso = Notificacion(
            id_usuario=usuario.id_usuario,
            id_camada=camada.id_camada,
            tipo=TIPO_PRODUCCION_BAJA,
            titulo="Producción baja",
            mensaje=(
                f"{camada.nombre_camada}: {produccion.total_huevos} huevos "
                f"ayer para {aves} aves ({tasa:.0%} de postura). "
                "Revisa la camada."
            ),
            fecha_referencia=fecha,
        )
    db.add(aviso)
    db.commit()


def _aves_en_fecha(db: Session, camada: Camada, fecha: date) -> int:
    """Cuenta las aves que había en la camada al cierre de la fecha.

    La cantidad actual descuenta las mortalidades ya registradas; se
    suman las que ocurrieron después de la fecha evaluada.
    """
    muertes_posteriores = (
        db.query(func.coalesce(func.sum(EventoSanitario.mortalidad), 0))
        .filter(
            EventoSanitario.id_camada == camada.id_camada,
            EventoSanitario.tipo_evento == "mortalidad",
            EventoSanitario.fecha_evento > fecha,
        )
        .scalar()
    )
    return int(camada.cantidad_actual + int(muertes_posteriores or 0))


def _existe_aviso_produccion(
    db: Session, usuario: Usuario, camada: Camada, fecha: date
) -> bool:
    """Indica si ya hay un aviso de producción para esa camada y fecha.

    Incluye los eliminados: si el usuario descartó el aviso, no se vuelve
    a crear para el mismo día.
    """
    return (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.id_camada == camada.id_camada,
            Notificacion.tipo.in_((TIPO_PRODUCCION_BAJA, TIPO_PRODUCCION_SIN_REGISTRO)),
            Notificacion.fecha_referencia == fecha,
        )
        .first()
        is not None
    )


def _cerrar_aviso(db: Session, aviso: Notificacion | None) -> None:
    """Marca como eliminado un aviso de stock que ya no aplica.

    Limpia `stock_referencia` para que un descenso posterior del stock
    vuelva a generar el aviso.
    """
    if aviso is not None and not aviso.eliminada:
        aviso.eliminada = 1
        aviso.stock_referencia = None
        db.commit()


def _tipo_aviso_stock(insumo: Insumo) -> str | None:
    """Devuelve el tipo de aviso según el stock, o None si está óptimo."""
    if insumo.stock_actual == 0:
        return TIPO_SIN_STOCK
    if insumo.stock_actual < insumo.umbral_minimo:
        return TIPO_STOCK_BAJO
    if insumo.stock_actual == insumo.umbral_minimo:
        return TIPO_STOCK_MINIMO
    return None


def _texto_aviso_stock(insumo: Insumo, tipo: str) -> tuple[str, str]:
    """Devuelve el título y el mensaje del aviso de stock."""
    unidad = insumo.unidad_medida
    if tipo == TIPO_SIN_STOCK:
        return "Sin stock", f"{insumo.nombre_insumo} se quedó sin stock"
    if tipo == TIPO_STOCK_BAJO:
        return (
            "Stock por debajo del mínimo",
            f"{insumo.nombre_insumo} está por debajo del mínimo "
            f"({insumo.stock_actual} de {insumo.umbral_minimo} {unidad})",
        )
    return (
        "Stock mínimo",
        f"{insumo.nombre_insumo} llegó al stock mínimo "
        f"({insumo.stock_actual} {unidad})",
    )


def _obtener_notificacion(
    db: Session, usuario: Usuario, id_notificacion: int
) -> Notificacion:
    """Busca una notificación del usuario o lanza 404."""
    notificacion = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_notificacion == id_notificacion,
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
        )
        .first()
    )
    if notificacion is None:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    return notificacion

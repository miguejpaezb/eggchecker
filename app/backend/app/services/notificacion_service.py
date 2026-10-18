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
    MOMENTO_HOY,
    MOMENTO_PREVIO,
    TIPO_CAMADA_LIMITE,
    TIPO_PRODUCCION_BAJA,
    TIPO_PRODUCCION_SIN_REGISTRO,
    TIPO_RECORDATORIO_SANITARIO,
    TIPO_RESUMEN_PERIODICO,
    TIPO_SIN_STOCK,
    TIPO_STOCK_BAJO,
    TIPO_STOCK_MINIMO,
    TIPOS_STOCK,
    Notificacion,
)
from app.models.produccion_diaria import ProduccionDiaria
from app.models.recordatorio_dosis import RecordatorioDosis
from app.models.recordatorio_sanitario import RecordatorioSanitario
from app.models.usuario import Usuario
from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

# Postura mínima esperada: por debajo del 85% de las aves activas se avisa.
UMBRAL_POSTURA = Decimal("0.85")

# Días que abarca el reporte de cada frecuencia del recordatorio.
DIAS_POR_FRECUENCIA = {"semanal": 7, "quincenal": 14, "mensual": 30}

# Lunes de referencia para alternar la cadencia quincenal.
EPOCH_QUINCENAL = date(2026, 1, 5)


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
    _generar_para_recordatorios(db, usuario)
    _generar_para_resumen(db, usuario)
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
    _generar_para_recordatorios(db, usuario)
    _generar_para_resumen(db, usuario)
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


def _generar_para_recordatorios(db: Session, usuario: Usuario) -> None:
    """Avisa el día antes y el mismo día de cada aplicación programada.

    Un aviso por recordatorio y fecha, no por camada: en un recordatorio
    global el mensaje resume cuántas camadas faltan por aplicar. Respeta
    la preferencia `notif_vacunacion` y no repite avisos descartados.
    """
    if not usuario.notif_vacunacion:
        return
    # Import local: recordatorio_service importa este módulo a su vez.
    from app.services import recordatorio_service

    recordatorio_service.extender_ocurrencias(db, usuario)
    hoy = date.today()
    manana = hoy + timedelta(days=1)
    filas = (
        db.query(RecordatorioDosis, RecordatorioSanitario)
        .join(
            RecordatorioSanitario,
            RecordatorioSanitario.id_recordatorio == RecordatorioDosis.id_recordatorio,
        )
        .filter(
            RecordatorioSanitario.id_usuario == usuario.id_usuario,
            RecordatorioSanitario.activo.is_(True),
            RecordatorioDosis.aplicada.is_(False),
            RecordatorioDosis.fecha_programada.in_((hoy, manana)),
        )
        .all()
    )
    grupos: dict[tuple[int, date], list[RecordatorioDosis]] = {}
    for dosis, regla in filas:
        grupos.setdefault((regla.id_recordatorio, dosis.fecha_programada), []).append(
            dosis
        )

    creada = False
    for (id_recordatorio, fecha), dosis_grupo in grupos.items():
        momento = MOMENTO_PREVIO if fecha == manana else MOMENTO_HOY
        if _existe_aviso_recordatorio(db, usuario, id_recordatorio, fecha, momento):
            continue
        regla = db.get(RecordatorioSanitario, id_recordatorio)
        insumo = db.get(Insumo, regla.id_insumo)
        if insumo is None:
            continue
        camadas = [db.get(Camada, dosis.id_camada) for dosis in dosis_grupo]
        camadas = [camada for camada in camadas if camada is not None]
        titulo, mensaje = _texto_recordatorio(
            db, regla, insumo, dosis_grupo, camadas, momento
        )
        db.add(
            Notificacion(
                id_usuario=usuario.id_usuario,
                id_camada=regla.id_camada,
                tipo=TIPO_RECORDATORIO_SANITARIO,
                titulo=titulo,
                mensaje=mensaje,
                fecha_referencia=fecha,
                momento=momento,
                id_recordatorio=id_recordatorio,
            )
        )
        creada = True
    if creada:
        db.commit()


def _texto_recordatorio(
    db: Session,
    regla: RecordatorioSanitario,
    insumo: Insumo,
    dosis_grupo: list[RecordatorioDosis],
    camadas: list[Camada],
    momento: str,
) -> tuple[str, str]:
    """Arma el título y el mensaje del aviso de un recordatorio."""
    cuando = "Mañana" if momento == MOMENTO_PREVIO else "Hoy"
    cantidad = f"{regla.cantidad} {insumo.unidad_medida}".strip()
    titulo = f"Recordatorio: {insumo.nombre_insumo}"[:80]
    if regla.id_camada is not None and camadas and dosis_grupo:
        total = (
            db.query(func.count(RecordatorioDosis.id_dosis))
            .filter(
                RecordatorioDosis.id_recordatorio == regla.id_recordatorio,
                RecordatorioDosis.id_camada == camadas[0].id_camada,
            )
            .scalar()
        )
        dosis = dosis_grupo[0]
        mensaje = (
            f"{cuando}: aplicar {cantidad} de {insumo.nombre_insumo} a "
            f"{camadas[0].nombre_camada} (dosis {dosis.numero_dosis} de {total})."
        )
        return titulo, mensaje
    nombres = ", ".join(camada.nombre_camada for camada in camadas[:3])
    if len(camadas) > 3:
        nombres = f"{len(camadas)} camadas"
    return titulo, (
        f"{cuando}: aplicar {cantidad} de {insumo.nombre_insumo} a {nombres}."
    )


def _existe_aviso_recordatorio(
    db: Session,
    usuario: Usuario,
    id_recordatorio: int,
    fecha: date,
    momento: str,
) -> bool:
    """Indica si ya se avisó esa dosis (incluye avisos descartados)."""
    return (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.tipo == TIPO_RECORDATORIO_SANITARIO,
            Notificacion.id_recordatorio == id_recordatorio,
            Notificacion.fecha_referencia == fecha,
            Notificacion.momento == momento,
        )
        .first()
        is not None
    )


def _generar_para_resumen(db: Session, usuario: Usuario) -> None:
    """Recuerda revisar los reportes según la frecuencia elegida.

    Solo crea el aviso de la ocurrencia más reciente (no acumula semanas
    atrás) y no lo repite si ya existe o si el usuario lo descartó. El
    rango del reporte se deduce de la frecuencia: 7, 14 o 30 días que
    terminan en `fecha_referencia`.
    """
    if not usuario.notif_resumen_semanal:
        return
    hoy = date.today()
    ocurrencia = _ocurrencia_resumen(usuario, hoy)
    if _existe_aviso_resumen(db, usuario, ocurrencia):
        return
    dias = DIAS_POR_FRECUENCIA.get(usuario.notif_resumen_frecuencia, 7)
    desde = ocurrencia - timedelta(days=dias - 1)
    db.add(
        Notificacion(
            id_usuario=usuario.id_usuario,
            tipo=TIPO_RESUMEN_PERIODICO,
            titulo="Recordatorio de reportes",
            mensaje=(
                f"Revisa el reporte del {desde:%d/%m} al {ocurrencia:%d/%m}. "
                "Puedes abrirlo cuando quieras."
            ),
            fecha_referencia=ocurrencia,
        )
    )
    db.commit()


def _ocurrencia_resumen(usuario: Usuario, hoy: date) -> date:
    """Calcula la última fecha en que tocaba el recordatorio de reportes."""
    dia = usuario.notif_resumen_dia
    frecuencia = usuario.notif_resumen_frecuencia
    if frecuencia == "mensual":
        return _primer_dia_del_mes(hoy, dia)
    ultimo = hoy - timedelta(days=(hoy.weekday() - dia) % 7)
    if frecuencia == "quincenal":
        semanas = (ultimo - EPOCH_QUINCENAL).days // 7
        if semanas % 2:
            ultimo -= timedelta(days=7)
    return ultimo


def _primer_dia_del_mes(hoy: date, dia: int) -> date:
    """Primer `<día de la semana>` del mes, o del mes anterior si no llegó."""
    primero = date(hoy.year, hoy.month, 1)
    candidato = primero + timedelta(days=(dia - primero.weekday()) % 7)
    if candidato <= hoy:
        return candidato
    anterior = primero - timedelta(days=1)
    primero = date(anterior.year, anterior.month, 1)
    return primero + timedelta(days=(dia - primero.weekday()) % 7)


def _existe_aviso_resumen(db: Session, usuario: Usuario, fecha: date) -> bool:
    """Indica si ya se avisó ese recordatorio de reportes."""
    return (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.tipo == TIPO_RESUMEN_PERIODICO,
            Notificacion.fecha_referencia == fecha,
        )
        .first()
        is not None
    )


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

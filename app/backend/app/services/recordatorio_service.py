from datetime import date, datetime, timedelta
from decimal import Decimal

from app.models.camada import Camada
from app.models.categoria_insumo import CategoriaInsumo
from app.models.evento_sanitario import EventoSanitario
from app.models.insumo import Insumo
from app.models.movimiento_insumo import MovimientoInsumo
from app.models.recordatorio_dosis import RecordatorioDosis
from app.models.recordatorio_sanitario import (
    CATEGORIAS_SANITARIAS,
    DIAS_HORIZONTE,
    MODO_CADA_DIAS,
    MODO_DIAS_SEMANA,
    MODO_FECHAS,
    RecordatorioSanitario,
)
from app.models.usuario import Usuario
from app.schemas.inventario import MovimientoCreate
from app.schemas.recordatorio import RecordatorioRequest
from app.services import insumo_service, notificacion_service
from fastapi import HTTPException
from sqlalchemy import or_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session


def listar_recordatorios(
    db: Session,
    usuario: Usuario,
    id_camada: int | None = None,
    incluir_globales: bool = True,
) -> list[dict]:
    """Lista los recordatorios activos del usuario.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los recordatorios.
        id_camada: Si se indica, filtra los de esa camada.
        incluir_globales: Si True, suma los recordatorios de la granja.

    Returns:
        list[dict]: Recordatorios con los datos del insumo y la próxima
        aplicación.
    """
    consulta = db.query(RecordatorioSanitario).filter(
        RecordatorioSanitario.id_usuario == usuario.id_usuario,
        RecordatorioSanitario.activo.is_(True),
    )
    if id_camada is not None:
        if incluir_globales:
            consulta = consulta.filter(
                or_(
                    RecordatorioSanitario.id_camada == id_camada,
                    RecordatorioSanitario.id_camada.is_(None),
                )
            )
        else:
            consulta = consulta.filter(RecordatorioSanitario.id_camada == id_camada)
    reglas = consulta.order_by(RecordatorioSanitario.id_recordatorio).all()
    return [_serializar(db, regla) for regla in reglas]


def obtener_recordatorio(
    db: Session, usuario: Usuario, id_recordatorio: int
) -> RecordatorioSanitario:
    """Busca un recordatorio del usuario o lanza 404."""
    regla = (
        db.query(RecordatorioSanitario)
        .filter(
            RecordatorioSanitario.id_recordatorio == id_recordatorio,
            RecordatorioSanitario.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if regla is None:
        raise HTTPException(status_code=404, detail="Recordatorio no encontrado")
    return regla


def obtener_detalle(db: Session, usuario: Usuario, id_recordatorio: int) -> dict:
    """Devuelve un recordatorio listo para la respuesta HTTP."""
    regla = obtener_recordatorio(db, usuario, id_recordatorio)
    return _serializar(db, regla)


def crear_recordatorio(
    db: Session, usuario: Usuario, datos: RecordatorioRequest
) -> dict:
    """Crea la regla y materializa sus primeras ocurrencias.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        datos: Regla validada.

    Returns:
        dict: El recordatorio creado con su próxima dosis.

    Raises:
        HTTPException: 404 si el insumo o la camada no son del usuario;
            400 si el insumo no es medicamento/vitamina, está inactivo o
            la camada no está activa.
    """
    insumo = _validar_insumo(db, usuario, datos.id_insumo)
    _validar_camada(db, usuario, datos.id_camada)
    regla = RecordatorioSanitario(
        id_usuario=usuario.id_usuario,
        id_camada=datos.id_camada,
        id_insumo=insumo.id_insumo,
        modo=datos.modo,
        intervalo_dias=datos.intervalo_dias,
        dias_semana=_serializar_dias(datos.dias_semana),
        fechas_especificas=_serializar_fechas(datos.fechas),
        fecha_inicio=datos.fecha_inicio,
        fecha_fin=datos.fecha_fin,
        cantidad=datos.cantidad,
        activo=datos.activo,
    )
    db.add(regla)
    db.commit()
    db.refresh(regla)
    _generar_ocurrencias(db, regla)
    return _serializar(db, regla)


def actualizar_recordatorio(
    db: Session, usuario: Usuario, id_recordatorio: int, datos: RecordatorioRequest
) -> dict:
    """Reemplaza una regla y recalcula las ocurrencias futuras.

    Las dosis ya aplicadas (y las pendientes de fechas pasadas) se
    conservan como historial; solo se eliminan las pendientes de hoy en
    adelante, que se vuelven a generar con la regla nueva.
    """
    regla = obtener_recordatorio(db, usuario, id_recordatorio)
    insumo = _validar_insumo(db, usuario, datos.id_insumo)
    _validar_camada(db, usuario, datos.id_camada)

    _borrar_dosis_futuras(db, regla)
    regla.id_camada = datos.id_camada
    regla.id_insumo = insumo.id_insumo
    regla.modo = datos.modo
    regla.intervalo_dias = datos.intervalo_dias
    regla.dias_semana = _serializar_dias(datos.dias_semana)
    regla.fechas_especificas = _serializar_fechas(datos.fechas)
    regla.fecha_inicio = datos.fecha_inicio
    regla.fecha_fin = datos.fecha_fin
    regla.cantidad = datos.cantidad
    regla.activo = datos.activo
    db.commit()
    _generar_ocurrencias(db, regla)
    return _serializar(db, regla)


def eliminar_recordatorio(db: Session, usuario: Usuario, id_recordatorio: int) -> None:
    """Desactiva el recordatorio y descarta sus dosis pendientes.

    No se borra físicamente para conservar el historial de aplicaciones.
    """
    regla = obtener_recordatorio(db, usuario, id_recordatorio)
    _borrar_dosis_futuras(db, regla)
    regla.activo = False
    db.commit()


def listar_dosis(
    db: Session,
    usuario: Usuario,
    id_recordatorio: int,
    desde: date | None = None,
    hasta: date | None = None,
    solo_pendientes: bool = False,
) -> list[dict]:
    """Lista las aplicaciones programadas de un recordatorio.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del recordatorio.
        id_recordatorio: Recordatorio consultado.
        desde: Fecha mínima (por defecto, todas).
        hasta: Fecha máxima (por defecto, todas).
        solo_pendientes: Si True, excluye las ya aplicadas.

    Returns:
        list[dict]: Dosis ordenadas por fecha y camada.
    """
    regla = obtener_recordatorio(db, usuario, id_recordatorio)
    consulta = db.query(RecordatorioDosis).filter(
        RecordatorioDosis.id_recordatorio == regla.id_recordatorio
    )
    if desde is not None:
        consulta = consulta.filter(RecordatorioDosis.fecha_programada >= desde)
    if hasta is not None:
        consulta = consulta.filter(RecordatorioDosis.fecha_programada <= hasta)
    if solo_pendientes:
        consulta = consulta.filter(RecordatorioDosis.aplicada.is_(False))
    dosis = consulta.order_by(
        RecordatorioDosis.fecha_programada, RecordatorioDosis.id_camada
    ).all()
    return [_serializar_dosis(db, item) for item in dosis]


def extender_ocurrencias(db: Session, usuario: Usuario) -> None:
    """Completa el horizonte de dosis de los recordatorios activos.

    Se usa en la sincronización perezosa: cubre las camadas que se
    crearon después del recordatorio global y los días que pasaron.
    """
    reglas = (
        db.query(RecordatorioSanitario)
        .filter(
            RecordatorioSanitario.id_usuario == usuario.id_usuario,
            RecordatorioSanitario.activo.is_(True),
        )
        .all()
    )
    for regla in reglas:
        _generar_ocurrencias(db, regla)


def obtener_dosis_detalle(db: Session, usuario: Usuario, id_dosis: int) -> dict:
    """Devuelve una dosis con su insumo y stock para registrar la aplicación."""
    dosis, regla = _obtener_dosis(db, usuario, id_dosis)
    insumo = db.get(Insumo, regla.id_insumo)
    camada = db.get(Camada, dosis.id_camada)
    return {
        "id_dosis": dosis.id_dosis,
        "id_recordatorio": regla.id_recordatorio,
        "id_camada": dosis.id_camada,
        "nombre_camada": camada.nombre_camada if camada else "",
        "fecha_programada": dosis.fecha_programada,
        "numero_dosis": dosis.numero_dosis,
        "aplicada": dosis.aplicada,
        "fecha_aplicacion": dosis.fecha_aplicacion,
        "id_insumo": regla.id_insumo,
        "nombre_insumo": insumo.nombre_insumo if insumo else "",
        "unidad_medida": insumo.unidad_medida if insumo else "",
        "cantidad": regla.cantidad,
        "stock_actual": insumo.stock_actual if insumo else Decimal("0"),
        "id_movimiento": dosis.id_movimiento,
        "id_evento": dosis.id_evento,
    }


def aplicar_dosis(
    db: Session,
    usuario: Usuario,
    id_dosis: int,
    cantidad: Decimal | None = None,
) -> dict:
    """Registra la aplicación de una dosis en un solo commit.

    Descuenta el insumo del inventario, deja el evento en el historial
    sanitario de la camada y marca la dosis como aplicada.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del recordatorio.
        id_dosis: Dosis programada a aplicar.
        cantidad: Cantidad realmente usada; sin ella se usa la del plan.

    Returns:
        dict: Dosis aplicada con el stock resultante y los ids creados.

    Raises:
        HTTPException: 404 si la dosis no es del usuario; 409 si ya estaba
            aplicada; 400 si todavía no es la fecha de aplicación, si la
            camada no está activa o si no hay stock.
    """
    dosis, regla = _obtener_dosis(db, usuario, id_dosis)
    _validar_dosis_pendiente(dosis)
    _validar_fecha_aplicacion(dosis)
    camada = _validar_camada_activa(db, usuario, dosis.id_camada)
    insumo = _validar_insumo(db, usuario, regla.id_insumo)
    usada = cantidad if cantidad is not None else regla.cantidad

    movimiento, evento, stock_resultante = _preparar_aplicacion(
        db, insumo, camada, regla, usada
    )
    db.flush()
    _marcar_aplicada(dosis, movimiento, evento)
    db.flush()
    notificacion_service.actualizar_aviso_recordatorio(
        db, usuario.id_usuario, regla.id_recordatorio, dosis.fecha_programada
    )
    _confirmar(db)
    notificacion_service.sincronizar_aviso_stock(db, insumo)
    return _serializar_aplicacion(
        db, dosis, regla, insumo, camada, usada, stock_resultante, movimiento
    )


def aplicar_dosis_lote(
    db: Session,
    usuario: Usuario,
    ids_dosis: list[int],
    cantidad: Decimal | None = None,
) -> list[dict]:
    """Aplica varias dosis en una sola transacción.

    Pensado para el recordatorio global: si una dosis falla (sin stock o
    ya aplicada) no se registra ninguna.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los recordatorios.
        ids_dosis: Dosis a aplicar.
        cantidad: Cantidad usada por camada; sin ella se usa la del plan.

    Returns:
        list[dict]: Resultado de cada aplicación.

    Raises:
        HTTPException: 404 si alguna dosis no es del usuario; 409 si ya
            estaba aplicada; 400 si todavía no es la fecha de aplicación,
            si la camada no está activa o si no hay stock.
    """
    preparadas = []
    for id_dosis in ids_dosis:
        dosis, regla = _obtener_dosis(db, usuario, id_dosis)
        _validar_dosis_pendiente(dosis)
        _validar_fecha_aplicacion(dosis)
        camada = _validar_camada_activa(db, usuario, dosis.id_camada)
        insumo = _validar_insumo(db, usuario, regla.id_insumo)
        usada = cantidad if cantidad is not None else regla.cantidad
        movimiento, evento, stock_resultante = _preparar_aplicacion(
            db, insumo, camada, regla, usada
        )
        preparadas.append(
            (
                dosis,
                regla,
                insumo,
                camada,
                usada,
                stock_resultante,
                movimiento,
                evento,
            )
        )
    db.flush()
    for dosis, _, _, _, _, _, movimiento, evento in preparadas:
        _marcar_aplicada(dosis, movimiento, evento)
    db.flush()
    afectadas = {
        (regla.id_recordatorio, dosis.fecha_programada)
        for dosis, regla, _, _, _, _, _, _ in preparadas
    }
    for id_recordatorio, fecha in afectadas:
        notificacion_service.actualizar_aviso_recordatorio(
            db, usuario.id_usuario, id_recordatorio, fecha
        )
    _confirmar(db)
    for insumo in {fila[2].id_insumo: fila[2] for fila in preparadas}.values():
        notificacion_service.sincronizar_aviso_stock(db, insumo)
    return [
        _serializar_aplicacion(
            db, dosis, regla, insumo, camada, usada, stock, movimiento
        )
        for dosis, regla, insumo, camada, usada, stock, movimiento, _ in preparadas
    ]


def _preparar_aplicacion(
    db: Session,
    insumo: Insumo,
    camada: Camada,
    regla: RecordatorioSanitario,
    cantidad: Decimal,
) -> tuple[MovimientoInsumo, EventoSanitario, Decimal]:
    """Arma el movimiento de salida y el evento sanitario sin commitear."""
    movimiento, stock_resultante = insumo_service.preparar_movimiento(
        db,
        insumo,
        MovimientoCreate(
            tipo_movimiento="salida",
            cantidad=cantidad,
            observaciones=(
                f"Recordatorio sanitario: {insumo.nombre_insumo} "
                f"en {camada.nombre_camada}"
            ),
        ),
    )
    categoria = db.get(CategoriaInsumo, insumo.id_categoria)
    tipo_evento = "vacunacion"
    if categoria is not None and categoria.nombre_categ != "medicamento":
        tipo_evento = "otro"
    evento = EventoSanitario(
        id_camada=camada.id_camada,
        tipo_evento=tipo_evento,
        fecha_evento=date.today(),
        descripcion=(
            f"{insumo.nombre_insumo}: {cantidad} {insumo.unidad_medida} "
            f"(recordatorio #{regla.id_recordatorio})"
        ),
        mortalidad=0,
    )
    db.add(evento)
    return movimiento, evento, stock_resultante


def _marcar_aplicada(
    dosis: RecordatorioDosis,
    movimiento: MovimientoInsumo,
    evento: EventoSanitario,
) -> None:
    """Marca la dosis como aplicada con su movimiento y evento."""
    dosis.aplicada = True
    dosis.fecha_aplicacion = datetime.now()
    dosis.id_movimiento = movimiento.id_movimiento
    dosis.id_evento = evento.id_evento


def _confirmar(db: Session) -> None:
    """Confirma la transacción y revierte si el stock quedó inconsistente."""
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(
            status_code=400,
            detail="No se pudo registrar la aplicación: stock inconsistente",
        ) from exc


def _serializar_aplicacion(
    db: Session,
    dosis: RecordatorioDosis,
    regla: RecordatorioSanitario,
    insumo: Insumo,
    camada: Camada,
    cantidad: Decimal,
    stock_resultante: Decimal,
    movimiento,
) -> dict:
    """Arma la respuesta de una dosis aplicada."""
    db.refresh(dosis)
    return {
        "id_dosis": dosis.id_dosis,
        "id_recordatorio": regla.id_recordatorio,
        "id_camada": dosis.id_camada,
        "nombre_camada": camada.nombre_camada,
        "id_insumo": insumo.id_insumo,
        "nombre_insumo": insumo.nombre_insumo,
        "unidad_medida": insumo.unidad_medida,
        "cantidad": cantidad,
        "stock_resultante": stock_resultante,
        "fecha_programada": dosis.fecha_programada,
        "fecha_aplicacion": dosis.fecha_aplicacion,
        "id_movimiento": movimiento.id_movimiento,
        "id_evento": dosis.id_evento,
    }


def _obtener_dosis(
    db: Session, usuario: Usuario, id_dosis: int
) -> tuple[RecordatorioDosis, RecordatorioSanitario]:
    """Busca una dosis y su regla dentro del inventario del usuario."""
    fila = (
        db.query(RecordatorioDosis, RecordatorioSanitario)
        .join(
            RecordatorioSanitario,
            RecordatorioSanitario.id_recordatorio == RecordatorioDosis.id_recordatorio,
        )
        .filter(
            RecordatorioDosis.id_dosis == id_dosis,
            RecordatorioSanitario.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if fila is None:
        raise HTTPException(status_code=404, detail="Dosis no encontrada")
    return fila[0], fila[1]


def _validar_dosis_pendiente(dosis: RecordatorioDosis) -> None:
    """Rechaza una dosis que ya fue aplicada."""
    if dosis.aplicada:
        raise HTTPException(status_code=409, detail="La dosis ya fue aplicada")


def _validar_fecha_aplicacion(dosis: RecordatorioDosis) -> None:
    """Rechaza aplicar una dosis antes de su fecha programada.

    Se permite el mismo día o después: si la aplicación se atrasó, el
    avicultor todavía puede registrarla.
    """
    if dosis.fecha_programada > date.today():
        raise HTTPException(
            status_code=400,
            detail="Todavía no es la fecha de aplicación",
        )


def _validar_camada_activa(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Devuelve la camada del usuario o lanza error si no aplica."""
    camada = db.get(Camada, id_camada)
    if camada is None or camada.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se aplican dosis en camadas activas",
        )
    return camada


def _generar_ocurrencias(db: Session, regla: RecordatorioSanitario) -> None:
    """Materializa las dosis pendientes dentro del horizonte."""
    camadas = _camadas_objetivo(db, regla)
    if not camadas:
        return
    hoy = date.today()
    limite = hoy + timedelta(days=DIAS_HORIZONTE)
    if regla.fecha_fin is not None and regla.fecha_fin < limite:
        limite = regla.fecha_fin
    if limite < hoy:
        return
    creada = False
    for camada in camadas:
        existentes = {
            fila[0]
            for fila in db.query(RecordatorioDosis.fecha_programada)
            .filter(
                RecordatorioDosis.id_recordatorio == regla.id_recordatorio,
                RecordatorioDosis.id_camada == camada.id_camada,
            )
            .all()
        }
        numero = len(existentes)
        for fecha in _fechas_programadas(regla, hoy, limite):
            if fecha in existentes:
                continue
            numero += 1
            db.add(
                RecordatorioDosis(
                    id_recordatorio=regla.id_recordatorio,
                    id_camada=camada.id_camada,
                    fecha_programada=fecha,
                    numero_dosis=numero,
                )
            )
            creada = True
    if creada:
        db.commit()


def _fechas_programadas(
    regla: RecordatorioSanitario, desde: date, hasta: date
) -> list[date]:
    """Calcula las fechas de aplicación de la regla en el rango."""
    inicio = max(regla.fecha_inicio, desde)
    if regla.modo == MODO_CADA_DIAS:
        return _fechas_cada_dias(regla, inicio, hasta)
    if regla.modo == MODO_DIAS_SEMANA:
        return _fechas_dias_semana(regla, inicio, hasta)
    if regla.modo == MODO_FECHAS:
        fechas = _parsear_fechas(regla.fechas_especificas)
        return [fecha for fecha in fechas if desde <= fecha <= hasta]
    return []


def _fechas_cada_dias(
    regla: RecordatorioSanitario, inicio: date, hasta: date
) -> list[date]:
    """Respeta la cadencia desde la fecha de inicio original."""
    intervalo = regla.intervalo_dias or 0
    if intervalo <= 0 or hasta < inicio:
        return []
    transcurridos = (inicio - regla.fecha_inicio).days
    resto = transcurridos % intervalo
    actual = inicio if resto == 0 else inicio + timedelta(days=intervalo - resto)
    fechas: list[date] = []
    while actual <= hasta:
        fechas.append(actual)
        actual += timedelta(days=intervalo)
    return fechas


def _fechas_dias_semana(
    regla: RecordatorioSanitario, inicio: date, hasta: date
) -> list[date]:
    """Devuelve los días de la semana elegidos dentro del rango."""
    dias = set(_parsear_dias(regla.dias_semana))
    fechas: list[date] = []
    actual = inicio
    while actual <= hasta:
        if actual.weekday() in dias:
            fechas.append(actual)
        actual += timedelta(days=1)
    return fechas


def _camadas_objetivo(db: Session, regla: RecordatorioSanitario) -> list[Camada]:
    """Camadas a las que aplica la regla (una o todas las activas)."""
    consulta = db.query(Camada).filter(Camada.estado == "activa")
    if regla.id_camada is not None:
        consulta = consulta.filter(Camada.id_camada == regla.id_camada)
    else:
        consulta = consulta.filter(Camada.id_usuario == regla.id_usuario)
    return consulta.all()


def _borrar_dosis_futuras(db: Session, regla: RecordatorioSanitario) -> None:
    """Elimina las dosis pendientes de hoy en adelante."""
    db.query(RecordatorioDosis).filter(
        RecordatorioDosis.id_recordatorio == regla.id_recordatorio,
        RecordatorioDosis.aplicada.is_(False),
        RecordatorioDosis.fecha_programada >= date.today(),
    ).delete(synchronize_session=False)
    db.commit()


def _validar_insumo(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Valida que el insumo sea un medicamento o vitamina del usuario."""
    insumo = db.get(Insumo, id_insumo)
    if insumo is None or insumo.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Insumo no encontrado")
    if insumo.descontinuado or not insumo.activo:
        raise HTTPException(
            status_code=400, detail="El insumo está inactivo o descontinuado"
        )
    categoria = db.get(CategoriaInsumo, insumo.id_categoria)
    if categoria is None or categoria.nombre_categ not in CATEGORIAS_SANITARIAS:
        raise HTTPException(
            status_code=400,
            detail="El insumo debe ser un medicamento o una vitamina",
        )
    return insumo


def _validar_camada(db: Session, usuario: Usuario, id_camada: int | None) -> None:
    """Valida que la camada exista, sea del usuario y esté activa."""
    if id_camada is None:
        return
    camada = db.get(Camada, id_camada)
    if camada is None or camada.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400, detail="Solo se programan recordatorios en camadas activas"
        )


def _serializar(db: Session, regla: RecordatorioSanitario) -> dict:
    """Arma la respuesta del recordatorio con datos de insumo y camada."""
    insumo = db.get(Insumo, regla.id_insumo)
    camada = db.get(Camada, regla.id_camada) if regla.id_camada else None
    pendientes = (
        db.query(RecordatorioDosis)
        .filter(
            RecordatorioDosis.id_recordatorio == regla.id_recordatorio,
            RecordatorioDosis.aplicada.is_(False),
            RecordatorioDosis.fecha_programada >= date.today(),
        )
        .all()
    )
    proxima = min((dosis.fecha_programada for dosis in pendientes), default=None)
    return {
        "id_recordatorio": regla.id_recordatorio,
        "id_camada": regla.id_camada,
        "nombre_camada": camada.nombre_camada if camada else None,
        "id_insumo": regla.id_insumo,
        "nombre_insumo": insumo.nombre_insumo if insumo else "",
        "unidad_medida": insumo.unidad_medida if insumo else "",
        "stock_actual": insumo.stock_actual if insumo else Decimal("0"),
        "cantidad": regla.cantidad,
        "modo": regla.modo,
        "intervalo_dias": regla.intervalo_dias,
        "dias_semana": _parsear_dias(regla.dias_semana) or None,
        "fechas": _parsear_fechas(regla.fechas_especificas) or None,
        "fecha_inicio": regla.fecha_inicio,
        "fecha_fin": regla.fecha_fin,
        "activo": regla.activo,
        "proxima_dosis": proxima,
        "dosis_pendientes": len(pendientes),
        "fecha_creacion": regla.fecha_creacion,
    }


def _serializar_dosis(db: Session, dosis: RecordatorioDosis) -> dict:
    """Arma la respuesta de una dosis con el nombre de su camada."""
    camada = db.get(Camada, dosis.id_camada)
    return {
        "id_dosis": dosis.id_dosis,
        "id_recordatorio": dosis.id_recordatorio,
        "id_camada": dosis.id_camada,
        "nombre_camada": camada.nombre_camada if camada else "",
        "fecha_programada": dosis.fecha_programada,
        "numero_dosis": dosis.numero_dosis,
        "aplicada": dosis.aplicada,
        "fecha_aplicacion": dosis.fecha_aplicacion,
        "id_movimiento": dosis.id_movimiento,
        "id_evento": dosis.id_evento,
    }


def _serializar_dias(dias: list[int] | None) -> str | None:
    """Convierte los días de la semana a texto separado por coma."""
    if not dias:
        return None
    return ",".join(str(dia) for dia in sorted(set(dias)))


def _parsear_dias(texto: str | None) -> list[int]:
    """Convierte el texto de días en una lista de enteros."""
    if not texto:
        return []
    return [int(parte) for parte in texto.split(",") if parte.strip()]


def _serializar_fechas(fechas: list[date] | None) -> str | None:
    """Convierte las fechas exactas a texto ISO separado por coma."""
    if not fechas:
        return None
    return ",".join(fecha.isoformat() for fecha in sorted(set(fechas)))


def _parsear_fechas(texto: str | None) -> list[date]:
    """Convierte el texto de fechas ISO en una lista de fechas."""
    if not texto:
        return []
    return [date.fromisoformat(parte) for parte in texto.split(",") if parte]

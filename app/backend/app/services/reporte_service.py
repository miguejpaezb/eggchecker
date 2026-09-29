from collections import Counter, defaultdict
from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal

from app.models.camada import (
    EDAD_INICIAL_SEMANAS,
    EDAD_PRODUCCION_SEMANAS,
    Camada,
)
from app.models.categoria_insumo import CategoriaInsumo
from app.models.cliente import Cliente
from app.models.detalle_pedido import DetallePedido
from app.models.evento_sanitario import EventoSanitario
from app.models.insumo import Insumo
from app.models.movimiento_insumo import MovimientoInsumo
from app.models.pedido import Pedido
from app.models.produccion_detalle import ProduccionDetalle
from app.models.produccion_diaria import ProduccionDiaria
from app.models.stock_produccion import StockProduccion
from app.models.tipo_huevo import TipoHuevo
from app.models.usuario import Usuario
from app.schemas.reporte import (
    AlimentoReporte,
    CamadaReporte,
    ClienteVentaItem,
    CubetaTipoResponse,
    GananciaReporte,
    GastosReporte,
    InsumoConsumoItem,
    InventarioHuevoItem,
    InventarioReporte,
    PeriodoResponse,
    ProduccionCamadaResponse,
    ProduccionReporte,
    ProduccionTipoResponse,
    ReporteConsolidadoResponse,
    SaludReporte,
    SerieProduccionItem,
    SerieVentaItem,
    VentasReporte,
)
from app.services.produccion_service import HUEVOS_POR_CUBETA
from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

_CERO = Decimal("0.00")
_TOPE_CLIENTES = 5
_TIPO_NO_APTO = "No_apto"
_CATEGORIA_ALIMENTO = "alimento"
_SEMANAS_HASTA_PRODUCCION = EDAD_PRODUCCION_SEMANAS - EDAD_INICIAL_SEMANAS


def obtener_consolidado(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    agrupacion: str,
    camada_id: int | None = None,
) -> ReporteConsolidadoResponse:
    """Construye el reporte de rentabilidad consolidado de un período.

    Agrega producción, ventas, gasto de insumos, ganancia, salud, consumo
    de alimento e inventario del usuario entre `desde` y `hasta` (ambos
    inclusive). Si se indica `camada_id`, la producción y la salud se
    acotan a esa camada y se añade su ficha; las ventas y los gastos
    siguen siendo de toda la granja porque no están asociados a camadas.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los datos.
        desde: Fecha inicial del período.
        hasta: Fecha final del período.
        agrupacion: 'dia', 'semana' o 'mes'.
        camada_id: Camada a la que acotar el reporte, si aplica.

    Returns:
        ReporteConsolidadoResponse: El consolidado listo para serializar.

    Raises:
        HTTPException: 400 si `desde` es posterior a `hasta`; 404 si la
            camada indicada no existe o no es del usuario.
    """
    if desde > hasta:
        raise HTTPException(
            status_code=400,
            detail="La fecha inicial no puede ser posterior a la final",
        )

    camada = _obtener_camada(db, usuario, camada_id) if camada_id else None
    dias = (hasta - desde).days + 1

    produccion = _produccion(db, usuario, desde, hasta, agrupacion, camada_id)
    ventas = _ventas(db, usuario, desde, hasta, agrupacion)
    gastos, alimento = _consumo(db, usuario, desde, hasta, dias)
    salud = _salud(db, usuario, desde, hasta, camada_id)
    inventario = _inventario(db, usuario)

    return ReporteConsolidadoResponse(
        periodo=PeriodoResponse(desde=desde, hasta=hasta, agrupacion=agrupacion),
        produccion=produccion,
        ventas=ventas,
        gastos=gastos,
        ganancia=_ganancia(ventas.ingreso_total, gastos),
        salud=salud,
        alimento=alimento,
        inventario=inventario,
        camada=(
            _ficha_camada(db, usuario, camada, desde, hasta)
            if camada is not None
            else None
        ),
    )


def _produccion(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    agrupacion: str,
    camada_id: int | None,
) -> ProduccionReporte:
    """Agrega la producción del período por tipo, camada y serie temporal."""
    consulta = db.query(
        ProduccionDiaria.fecha_recoleccion,
        ProduccionDiaria.total_huevos,
    ).filter(
        ProduccionDiaria.id_usuario == usuario.id_usuario,
        ProduccionDiaria.fecha_recoleccion >= desde,
        ProduccionDiaria.fecha_recoleccion <= hasta,
    )
    if camada_id is not None:
        consulta = consulta.filter(ProduccionDiaria.id_camada == camada_id)
    filas = consulta.all()

    total = sum(fila.total_huevos for fila in filas)
    dias = {fila.fecha_recoleccion for fila in filas}
    promedio = round(total / len(dias), 2) if dias else 0.0
    por_tipo = _produccion_por_tipo(db, usuario, desde, hasta, camada_id)

    return ProduccionReporte(
        total_huevos=total,
        cubetas_completas=total // HUEVOS_POR_CUBETA,
        huevos_no_aptos=_cantidad_tipo(por_tipo, _TIPO_NO_APTO),
        promedio_diario=promedio,
        por_tipo=por_tipo,
        cubetas_por_tipo=[
            CubetaTipoResponse(
                nombre_tipo=item.nombre_tipo,
                cubetas=item.cantidad // HUEVOS_POR_CUBETA,
            )
            for item in por_tipo
            if item.nombre_tipo != _TIPO_NO_APTO
        ],
        por_camada=_produccion_por_camada(db, usuario, desde, hasta, camada_id),
        serie=[
            SerieProduccionItem(periodo=periodo, total_huevos=valor)
            for periodo, valor in _serie(
                filas, agrupacion, lambda fila: fila.total_huevos
            )
        ],
    )


def _produccion_por_tipo(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    camada_id: int | None,
) -> list[ProduccionTipoResponse]:
    """Suma la producción del período por tipo de huevo (4 tipos fijos)."""
    por_tipo = {
        tipo.nombre_tipo: 0
        for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all()
    }
    consulta = (
        db.query(TipoHuevo.nombre_tipo, func.sum(ProduccionDetalle.cantidad))
        .join(ProduccionDetalle, ProduccionDetalle.id_tipo == TipoHuevo.id_tipo)
        .join(
            ProduccionDiaria,
            ProduccionDetalle.id_produccion == ProduccionDiaria.id_produccion,
        )
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= desde,
            ProduccionDiaria.fecha_recoleccion <= hasta,
        )
    )
    if camada_id is not None:
        consulta = consulta.filter(ProduccionDiaria.id_camada == camada_id)
    for nombre, cantidad in consulta.group_by(TipoHuevo.nombre_tipo).all():
        por_tipo[nombre] = int(cantidad or 0)
    return [
        ProduccionTipoResponse(nombre_tipo=nombre, cantidad=cantidad)
        for nombre, cantidad in por_tipo.items()
    ]


def _produccion_por_camada(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    camada_id: int | None,
) -> list[ProduccionCamadaResponse]:
    """Suma la producción del período por camada, de mayor a menor."""
    consulta = (
        db.query(
            Camada.id_camada,
            Camada.nombre_camada,
            func.sum(ProduccionDiaria.total_huevos),
        )
        .join(ProduccionDiaria, ProduccionDiaria.id_camada == Camada.id_camada)
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= desde,
            ProduccionDiaria.fecha_recoleccion <= hasta,
        )
    )
    if camada_id is not None:
        consulta = consulta.filter(Camada.id_camada == camada_id)
    filas = (
        consulta.group_by(Camada.id_camada, Camada.nombre_camada)
        .order_by(func.sum(ProduccionDiaria.total_huevos).desc())
        .all()
    )
    return [
        ProduccionCamadaResponse(
            id_camada=id_camada,
            nombre_camada=nombre,
            total_huevos=int(total or 0),
        )
        for id_camada, nombre, total in filas
    ]


def _ventas(
    db: Session, usuario: Usuario, desde: date, hasta: date, agrupacion: str
) -> VentasReporte:
    """Agrega las ventas del período excluyendo los pedidos cancelados."""
    filtro = _filtro_pedidos(usuario, desde, hasta)

    ingreso = _q(
        db.query(func.coalesce(func.sum(Pedido.valor_total), 0))
        .filter(*filtro)
        .scalar()
        or 0
    )
    num_pedidos = int(
        db.query(func.count(Pedido.id_pedido)).filter(*filtro).scalar() or 0
    )
    ticket = _q(ingreso / num_pedidos) if num_pedidos else _CERO

    filas = db.query(Pedido.fecha_pedido, Pedido.valor_total).filter(*filtro).all()

    return VentasReporte(
        ingreso_total=ingreso,
        unidades_totales=_unidades_vendidas(db, usuario, desde, hasta),
        num_pedidos=num_pedidos,
        ticket_promedio=ticket,
        por_tipo=_ventas_por_tipo(db, usuario, desde, hasta),
        top_clientes=_top_clientes(db, usuario, desde, hasta),
        serie=[
            SerieVentaItem(periodo=periodo, ingreso_total=_q(valor))
            for periodo, valor in _serie(
                filas, agrupacion, lambda fila: fila.valor_total
            )
        ],
    )


def _unidades_vendidas(db: Session, usuario: Usuario, desde: date, hasta: date) -> int:
    """Suma las unidades (huevos) vendidas en el período."""
    total = (
        db.query(func.coalesce(func.sum(DetallePedido.cantidad), 0))
        .join(Pedido, DetallePedido.id_pedido == Pedido.id_pedido)
        .filter(*_filtro_pedidos(usuario, desde, hasta))
        .scalar()
    )
    return int(total or 0)


def _ventas_por_tipo(
    db: Session, usuario: Usuario, desde: date, hasta: date
) -> list[ProduccionTipoResponse]:
    """Suma las unidades vendidas del período por tipo de huevo."""
    por_tipo = {
        tipo.nombre_tipo: 0
        for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all()
    }
    filas = (
        db.query(TipoHuevo.nombre_tipo, func.sum(DetallePedido.cantidad))
        .join(DetallePedido, DetallePedido.id_tipo == TipoHuevo.id_tipo)
        .join(Pedido, DetallePedido.id_pedido == Pedido.id_pedido)
        .filter(*_filtro_pedidos(usuario, desde, hasta))
        .group_by(TipoHuevo.nombre_tipo)
        .all()
    )
    for nombre, cantidad in filas:
        por_tipo[nombre] = int(cantidad or 0)
    return [
        ProduccionTipoResponse(nombre_tipo=nombre, cantidad=cantidad)
        for nombre, cantidad in por_tipo.items()
        if cantidad
    ]


def _top_clientes(
    db: Session, usuario: Usuario, desde: date, hasta: date
) -> list[ClienteVentaItem]:
    """Devuelve los clientes con mayor ingreso en el período."""
    filas = (
        db.query(
            Cliente.id_cliente,
            Cliente.nombre_cliente,
            func.sum(Pedido.valor_total),
            func.count(Pedido.id_pedido),
        )
        .join(Pedido, Pedido.id_cliente == Cliente.id_cliente)
        .filter(*_filtro_pedidos(usuario, desde, hasta))
        .group_by(Cliente.id_cliente, Cliente.nombre_cliente)
        .order_by(func.sum(Pedido.valor_total).desc())
        .limit(_TOPE_CLIENTES)
        .all()
    )
    return [
        ClienteVentaItem(
            id_cliente=id_cliente,
            nombre_cliente=nombre,
            ingreso_total=_q(ingreso or 0),
            num_pedidos=int(num_pedidos or 0),
        )
        for id_cliente, nombre, ingreso, num_pedidos in filas
    ]


def _consumo(
    db: Session, usuario: Usuario, desde: date, hasta: date, dias: int
) -> tuple[GastosReporte, AlimentoReporte]:
    """Valoriza el consumo de insumos (salidas) del período.

    El gasto de cada salida es `cantidad x costo promedio del insumo`.
    El consumo de la categoría 'alimento' se resume aparte para "Uso de
    comida" (kg usados y promedio diario).
    """
    insumos = db.query(Insumo).filter(Insumo.id_usuario == usuario.id_usuario).all()
    mapa = {insumo.id_insumo: insumo for insumo in insumos}
    categorias = _mapa_categorias(db)

    movimientos: list[MovimientoInsumo] = []
    if mapa:
        movimientos = (
            db.query(MovimientoInsumo)
            .filter(
                MovimientoInsumo.id_insumo.in_(mapa.keys()),
                MovimientoInsumo.tipo_movimiento == "salida",
                func.date(MovimientoInsumo.fecha_movimiento) >= desde.isoformat(),
                func.date(MovimientoInsumo.fecha_movimiento) <= hasta.isoformat(),
            )
            .all()
        )

    acumulado: dict[int, dict[str, Decimal]] = defaultdict(
        lambda: {"cantidad": _CERO, "costo": _CERO}
    )
    alimento: dict[int, dict[str, Decimal]] = defaultdict(
        lambda: {"cantidad": _CERO, "costo": _CERO}
    )
    for movimiento in movimientos:
        insumo = mapa[movimiento.id_insumo]
        costo_unitario = movimiento.costo_unitario
        if costo_unitario is None:
            costo_unitario = insumo.costo_unitario
        costo_unitario = costo_unitario or _CERO
        acumulado[movimiento.id_insumo]["cantidad"] += movimiento.cantidad
        acumulado[movimiento.id_insumo]["costo"] += movimiento.cantidad * costo_unitario
        if categorias.get(insumo.id_categoria) == _CATEGORIA_ALIMENTO:
            alimento[movimiento.id_insumo]["cantidad"] += movimiento.cantidad
            alimento[movimiento.id_insumo]["costo"] += (
                movimiento.cantidad * costo_unitario
            )

    gastos = GastosReporte(
        total=_q(sum(fila["costo"] for fila in acumulado.values())),
        por_insumo=_items_consumo(acumulado, mapa, categorias),
    )
    kg_usados = sum((fila["cantidad"] for fila in alimento.values()), _CERO)
    return gastos, AlimentoReporte(
        kg_usados=_q(kg_usados),
        promedio_diario=_q(kg_usados / dias) if dias else _CERO,
        por_insumo=_items_consumo(alimento, mapa, categorias),
    )


def _ganancia(ingreso: Decimal, gastos: GastosReporte) -> GananciaReporte:
    """Calcula la ganancia del período: ventas menos gastos."""
    valor = _q(ingreso - gastos.total)
    porcentaje = _q(valor / ingreso * 100) if ingreso > 0 else _CERO
    return GananciaReporte(valor=valor, porcentaje=porcentaje)


def _salud(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    camada_id: int | None,
) -> SaludReporte:
    """Resume bajas, causa principal y vacunación del período."""
    consulta = (
        db.query(
            EventoSanitario.tipo_evento,
            EventoSanitario.descripcion,
            EventoSanitario.mortalidad,
        )
        .join(Camada, EventoSanitario.id_camada == Camada.id_camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            EventoSanitario.fecha_evento >= desde,
            EventoSanitario.fecha_evento <= hasta,
        )
    )
    if camada_id is not None:
        consulta = consulta.filter(EventoSanitario.id_camada == camada_id)
    filas = consulta.all()

    causas = Counter(
        fila.descripcion
        for fila in filas
        if fila.tipo_evento in ("mortalidad", "enfermedad")
    )
    return SaludReporte(
        gallinas_perdidas=sum(int(fila.mortalidad or 0) for fila in filas),
        causa_principal=causas.most_common(1)[0][0] if causas else None,
        vacunacion_al_dia=any(fila.tipo_evento == "vacunacion" for fila in filas),
    )


def _inventario(db: Session, usuario: Usuario) -> InventarioReporte:
    """Arma la instantánea de inventario de huevos e insumos en alerta."""
    nombres = {
        tipo.id_tipo: tipo.nombre_tipo
        for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all()
    }
    registros = (
        db.query(StockProduccion)
        .filter(StockProduccion.id_usuario == usuario.id_usuario)
        .all()
    )
    huevos = [
        InventarioHuevoItem(
            id_tipo=registro.id_tipo,
            nombre_tipo=nombres.get(registro.id_tipo, ""),
            cantidad_actual=registro.cantidad_actual,
            valor_unidad=registro.valor_unidad,
            valor_total=_q(registro.valor_unidad * registro.cantidad_actual),
        )
        for registro in registros
        if registro.cantidad_actual > 0
    ]
    valor_stock = _q(sum((item.valor_total for item in huevos), _CERO))

    alertas = 0
    for insumo in (
        db.query(Insumo).filter(Insumo.id_usuario == usuario.id_usuario).all()
    ):
        if (
            insumo.activo
            and not insumo.descontinuado
            and insumo.stock_actual < insumo.umbral_minimo
        ):
            alertas += 1

    return InventarioReporte(
        huevos=huevos,
        valor_stock=valor_stock,
        insumos_bajo_umbral=alertas,
    )


def _ficha_camada(
    db: Session,
    usuario: Usuario,
    camada: Camada,
    desde: date,
    hasta: date,
) -> CamadaReporte:
    """Calcula los indicadores productivos de una camada específica."""
    fecha_produccion = camada.fecha_ingreso + timedelta(weeks=_SEMANAS_HASTA_PRODUCCION)
    mortalidad_antes = (
        db.query(func.coalesce(func.sum(EventoSanitario.mortalidad), 0))
        .filter(
            EventoSanitario.id_camada == camada.id_camada,
            EventoSanitario.fecha_evento <= fecha_produccion,
        )
        .scalar()
    )

    base = db.query(
        ProduccionDiaria.fecha_recoleccion,
        ProduccionDiaria.total_huevos,
    ).filter(
        ProduccionDiaria.id_usuario == usuario.id_usuario,
        ProduccionDiaria.id_camada == camada.id_camada,
    )
    en_rango = base.filter(
        ProduccionDiaria.fecha_recoleccion >= desde,
        ProduccionDiaria.fecha_recoleccion <= hasta,
    ).all()
    total_rango = sum(fila.total_huevos for fila in en_rango)
    dias_rango = {fila.fecha_recoleccion for fila in en_rango}

    desde_produccion = base.filter(
        ProduccionDiaria.fecha_recoleccion >= fecha_produccion
    ).all()
    huevos_produccion = sum(fila.total_huevos for fila in desde_produccion)
    dias_produccion = max((date.today() - fecha_produccion).days, 1)

    return CamadaReporte(
        id_camada=camada.id_camada,
        nombre_camada=camada.nombre_camada,
        fecha_ingreso=camada.fecha_ingreso,
        aves_semana_28=max(camada.cantidad_inicial - int(mortalidad_antes or 0), 0),
        edad_semanas=camada.edad_semanas,
        edad_dias=camada.edad_semanas * 7,
        total_huevos=total_rango,
        cubetas_completas=total_rango // HUEVOS_POR_CUBETA,
        promedio_diario=(
            round(total_rango / len(dias_rango), 2) if dias_rango else 0.0
        ),
        promedio_produccion=round(huevos_produccion / dias_produccion, 2),
    )


def _filtro_pedidos(usuario: Usuario, desde: date, hasta: date) -> list:
    """Filtros ORM compartidos para las ventas válidas del período."""
    return [
        Pedido.id_usuario == usuario.id_usuario,
        Pedido.estado_pedido != "cancelado",
        Pedido.fecha_pedido >= desde,
        Pedido.fecha_pedido <= hasta,
    ]


def _mapa_categorias(db: Session) -> dict[int, str]:
    """Devuelve el nombre de cada categoría de insumo indexado por id."""
    return {
        categoria.id_categoria: categoria.nombre_categ
        for categoria in db.query(CategoriaInsumo).all()
    }


def _items_consumo(
    acumulado: dict[int, dict[str, Decimal]],
    mapa: dict[int, Insumo],
    categorias: dict[int, str],
) -> list[InsumoConsumoItem]:
    """Convierte el consumo acumulado por insumo en items de respuesta."""
    items: list[InsumoConsumoItem] = []
    for id_insumo, fila in sorted(acumulado.items()):
        insumo = mapa[id_insumo]
        cantidad = fila["cantidad"]
        costo_total = _q(fila["costo"])
        costo_unitario = _q(fila["costo"] / cantidad) if cantidad > 0 else _CERO
        items.append(
            InsumoConsumoItem(
                id_insumo=id_insumo,
                nombre_insumo=insumo.nombre_insumo,
                categoria=categorias.get(insumo.id_categoria, ""),
                unidad_medida=insumo.unidad_medida,
                cantidad=cantidad,
                costo_unitario=costo_unitario,
                costo_total=costo_total,
            )
        )
    return items


def _cantidad_tipo(por_tipo: list[ProduccionTipoResponse], nombre_tipo: str) -> int:
    """Devuelve la cantidad de un tipo de huevo dentro de `por_tipo`."""
    return next(
        (item.cantidad for item in por_tipo if item.nombre_tipo == nombre_tipo),
        0,
    )


def _obtener_camada(db: Session, usuario: Usuario, id_camada: int | None) -> Camada:
    """Busca una camada del usuario o lanza 404 si no existe."""
    camada = (
        db.query(Camada)
        .filter(
            Camada.id_camada == id_camada,
            Camada.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if camada is None:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    return camada


def _serie(filas: list, agrupacion: str, valor) -> list[tuple[str, Decimal | int]]:
    """Agrupa filas (fecha, valor) por día, semana o mes.

    Args:
        filas: Filas con atributo de fecha en la primera columna.
        agrupacion: 'dia', 'semana' o 'mes'.
        valor: Función que extrae el valor numérico de cada fila.

    Returns:
        list[tuple[str, Decimal | int]]: Pares (etiqueta, suma) ordenados
        cronológicamente.
    """
    acumulado: dict[str, Decimal | int] = defaultdict(int)
    for fila in filas:
        etiqueta = _periodo(fila[0], agrupacion)
        acumulado[etiqueta] += valor(fila)
    return sorted(acumulado.items())


def _periodo(fecha: date, agrupacion: str) -> str:
    """Devuelve la etiqueta de período de una fecha según la agrupación."""
    if agrupacion == "mes":
        return f"{fecha.year}-{fecha.month:02d}"
    if agrupacion == "semana":
        iso = fecha.isocalendar()
        return f"{iso.year}-W{iso.week:02d}"
    return fecha.isoformat()


def _q(valor: Decimal | int | float) -> Decimal:
    """Redondea un valor monetario a dos decimales (half-up)."""
    return Decimal(str(valor)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal

from app.models.camada import Camada
from app.models.evento_sanitario import EventoSanitario
from app.models.produccion_detalle import ProduccionDetalle
from app.models.produccion_diaria import ProduccionDiaria
from app.models.stock_produccion import StockProduccion
from app.models.usuario import Usuario
from app.schemas.dashboard import (
    AlertaDashboard,
    DashboardResponse,
    DiaProduccion,
    MejorDia,
    PedidoReciente,
    SemanaDashboard,
)
from app.services import insumo_service, venta_service
from sqlalchemy import func
from sqlalchemy.orm import Session

# La ventana del resumen semanal cubre hoy y los seis días anteriores.
_DIAS_VENTANA = 7
# Estados de un pedido que todavía no se entrega.
_ESTADOS_PENDIENTES = ("pendiente", "enviado")
# Cantidad de pedidos recientes que muestra el dashboard.
_TOPE_PEDIDOS = 3
_CENTAVOS = Decimal("0.01")
_CERO = Decimal("0.00")

# Nombres cortos de los días, en el orden que devuelve date.weekday().
_DIAS_SEMANA = ("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")


def obtener_dashboard(db: Session, usuario: Usuario) -> DashboardResponse:
    """Construye los indicadores agregados del dashboard del avicultor.

    Reúne en una sola consulta lógica los KPI del día, el resumen de los
    últimos siete días, las alertas de insumos y los pedidos recientes.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado dueño de los datos.

    Returns:
        DashboardResponse: Los indicadores listos para serializar.
    """
    hoy = date.today()
    desde = hoy - timedelta(days=_DIAS_VENTANA - 1)

    produccion_hoy = _total_produccion(db, usuario, hoy)
    produccion_ayer = _total_produccion(db, usuario, hoy - timedelta(days=1))
    aves_activas = _aves_activas(db, usuario)
    pedidos = venta_service.listar_pedidos(db, usuario)
    alertas = insumo_service.listar_alertas(db, usuario)

    return DashboardResponse(
        produccion_hoy=produccion_hoy,
        variacion_produccion=_variacion(produccion_hoy, produccion_ayer),
        aves_activas=aves_activas,
        pedidos_pendientes=sum(
            1 for pedido in pedidos if pedido.estado_pedido in _ESTADOS_PENDIENTES
        ),
        alertas_count=len(alertas),
        semana=_semana(db, usuario, desde, hoy, aves_activas),
        alertas=[
            AlertaDashboard(
                id_insumo=alerta.id_insumo,
                nombre_insumo=alerta.nombre_insumo,
                categoria=alerta.categoria,
                stock_actual=alerta.stock_actual,
                umbral_minimo=alerta.umbral_minimo,
            )
            for alerta in alertas
        ],
        pedidos_recientes=[
            _pedido_reciente(pedido) for pedido in pedidos[:_TOPE_PEDIDOS]
        ],
    )


def _semana(
    db: Session,
    usuario: Usuario,
    desde: date,
    hasta: date,
    aves_activas: int,
) -> SemanaDashboard:
    """Arma la serie de siete días con su resumen productivo."""
    totales = _totales_por_dia(db, usuario, desde, hasta)
    serie = [
        DiaProduccion(
            fecha=dia,
            etiqueta=_DIAS_SEMANA[dia.weekday()],
            total_huevos=totales.get(dia, 0),
        )
        for dia in (desde + timedelta(days=offset) for offset in range(_DIAS_VENTANA))
    ]
    total = sum(item.total_huevos for item in serie)

    return SemanaDashboard(
        serie=serie,
        total_huevos=total,
        mejor_dia=_mejor_dia(serie, total),
        valor_producido=_valor_producido(db, usuario, desde, hasta),
        tasa_postura=_tasa_postura(total, aves_activas),
        mortalidad=_mortalidad(db, usuario, desde, hasta),
    )


def _total_produccion(db: Session, usuario: Usuario, dia: date) -> int:
    """Suma los huevos recolectados por el usuario en un día."""
    total = (
        db.query(func.coalesce(func.sum(ProduccionDiaria.total_huevos), 0))
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion == dia,
        )
        .scalar()
    )
    return int(total or 0)


def _totales_por_dia(
    db: Session, usuario: Usuario, desde: date, hasta: date
) -> dict[date, int]:
    """Devuelve los huevos recolectados por día dentro de la ventana."""
    filas = (
        db.query(
            ProduccionDiaria.fecha_recoleccion,
            func.sum(ProduccionDiaria.total_huevos),
        )
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= desde,
            ProduccionDiaria.fecha_recoleccion <= hasta,
        )
        .group_by(ProduccionDiaria.fecha_recoleccion)
        .all()
    )
    return {fecha: int(total or 0) for fecha, total in filas}


def _aves_activas(db: Session, usuario: Usuario) -> int:
    """Suma las aves actuales de todas las camadas activas del usuario."""
    total = (
        db.query(func.coalesce(func.sum(Camada.cantidad_actual), 0))
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            Camada.estado == "activa",
        )
        .scalar()
    )
    return int(total or 0)


def _valor_producido(
    db: Session, usuario: Usuario, desde: date, hasta: date
) -> Decimal:
    """Valora la producción de la ventana al precio de referencia por tipo.

    Se usan los `valor_unidad` vigentes de `stock_produccion` (el mismo
    precio que usa el reporte de inventario). Si un tipo no tiene precio
    de referencia configurado, aporta cero.
    """
    cantidades = dict(
        db.query(
            ProduccionDetalle.id_tipo,
            func.coalesce(func.sum(ProduccionDetalle.cantidad), 0),
        )
        .join(
            ProduccionDiaria,
            ProduccionDetalle.id_produccion == ProduccionDiaria.id_produccion,
        )
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= desde,
            ProduccionDiaria.fecha_recoleccion <= hasta,
        )
        .group_by(ProduccionDetalle.id_tipo)
        .all()
    )
    if not cantidades:
        return _CERO

    valores = dict(
        db.query(StockProduccion.id_tipo, StockProduccion.valor_unidad).filter(
            StockProduccion.id_usuario == usuario.id_usuario
        )
    )
    total = sum(
        (
            cantidad * valores.get(id_tipo, _CERO)
            for id_tipo, cantidad in cantidades.items()
        ),
        _CERO,
    )
    return _q(total)


def _tasa_postura(total_huevos: int, aves_activas: int) -> Decimal:
    """Calcula el porcentaje de postura de la ventana semanal.

    Adapta `calcular_porcentaje_postura` de la granja de referencia:
    `huevos / (aves x días) * 100`, con las aves actuales de las camadas
    activas como denominador.
    """
    if aves_activas <= 0:
        return _CERO
    return _q(Decimal(total_huevos) / (aves_activas * _DIAS_VENTANA) * 100)


def _mortalidad(db: Session, usuario: Usuario, desde: date, hasta: date) -> int:
    """Suma las aves muertas registradas en eventos sanitarios de la ventana."""
    total = (
        db.query(func.coalesce(func.sum(EventoSanitario.mortalidad), 0))
        .join(Camada, EventoSanitario.id_camada == Camada.id_camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            EventoSanitario.fecha_evento >= desde,
            EventoSanitario.fecha_evento <= hasta,
        )
        .scalar()
    )
    return int(total or 0)


def _mejor_dia(serie: list[DiaProduccion], total: int) -> MejorDia | None:
    """Devuelve el día de mayor recolección, o None si no hubo producción."""
    if total == 0:
        return None
    dia = max(serie, key=lambda item: item.total_huevos)
    return MejorDia(etiqueta=dia.etiqueta, total_huevos=dia.total_huevos)


def _variacion(hoy: int, ayer: int) -> Decimal | None:
    """Calcula la variación porcentual de la producción frente a ayer.

    Devuelve None cuando no hay base de comparación (ayer sin datos).
    """
    if ayer == 0:
        return None
    return _q(Decimal(hoy - ayer) / ayer * 100)


def _pedido_reciente(pedido) -> PedidoReciente:
    """Resume un pedido (cliente, líneas y estado) para el dashboard."""
    descripcion = " + ".join(
        f"{detalle.cantidad} {detalle.tipo.nombre_tipo}" for detalle in pedido.detalles
    )
    return PedidoReciente(
        id_pedido=pedido.id_pedido,
        cliente_nombre=pedido.cliente.nombre_cliente,
        descripcion=descripcion or "Sin detalle",
        fecha_pedido=pedido.fecha_pedido,
        estado_pedido=pedido.estado_pedido,
    )


def _q(valor: Decimal | int | float) -> Decimal:
    """Redondea un valor a dos decimales (half-up)."""
    return Decimal(str(valor)).quantize(_CENTAVOS, rounding=ROUND_HALF_UP)

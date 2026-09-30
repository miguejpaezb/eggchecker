from datetime import date
from decimal import Decimal

from pydantic import BaseModel


class AlertaDashboard(BaseModel):
    """Insumo en alerta mostrado en el resumen del dashboard."""

    id_insumo: int
    nombre_insumo: str
    categoria: str
    stock_actual: Decimal
    umbral_minimo: Decimal


class PedidoReciente(BaseModel):
    """Pedido reciente resumido para la tarjeta del dashboard."""

    id_pedido: int
    cliente_nombre: str
    descripcion: str
    fecha_pedido: date
    estado_pedido: str


class DiaProduccion(BaseModel):
    """Un día de la serie semanal de producción (con relleno en cero)."""

    fecha: date
    etiqueta: str
    total_huevos: int


class MejorDia(BaseModel):
    """Día de mayor recolección dentro de la ventana semanal."""

    etiqueta: str
    total_huevos: int


class SemanaDashboard(BaseModel):
    """Resumen de producción de los últimos siete días.

    `valor_producido` valora los huevos por el precio de referencia de
    cada tipo (`stock_produccion.valor_unidad`) y `tasa_postura` es el
    porcentaje de postura sobre las aves activas en la ventana.
    """

    serie: list[DiaProduccion]
    total_huevos: int
    mejor_dia: MejorDia | None
    valor_producido: Decimal
    tasa_postura: Decimal
    mortalidad: int


class DashboardResponse(BaseModel):
    """Indicadores agregados que alimentan la vista principal (RF-18).

    Reúne en una sola respuesta los KPI, la serie semanal, las alertas de
    insumos y los pedidos recientes del avicultor autenticado.
    """

    produccion_hoy: int
    variacion_produccion: Decimal | None
    aves_activas: int
    pedidos_pendientes: int
    alertas_count: int
    semana: SemanaDashboard
    alertas: list[AlertaDashboard]
    pedidos_recientes: list[PedidoReciente]

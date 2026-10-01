from datetime import date
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, ConfigDict

# Agrupación temporal de las series del reporte.
AGRUPACION = Literal["dia", "semana", "mes"]


class PeriodoResponse(BaseModel):
    """Rango de fechas y agrupación usados en el consolidado."""

    desde: date
    hasta: date
    agrupacion: str


class ProduccionTipoResponse(BaseModel):
    """Cantidad de huevos por tipo (AA, A, B, No_apto)."""

    nombre_tipo: str
    cantidad: int


class CubetaTipoResponse(BaseModel):
    """Cubetas completas (30 huevos) producidas por tipo."""

    nombre_tipo: str
    cubetas: int


class ProduccionCamadaResponse(BaseModel):
    """Total de huevos producidos por camada en el período."""

    id_camada: int
    nombre_camada: str
    total_huevos: int


class SerieProduccionItem(BaseModel):
    """Punto de la serie de producción para un período agrupado."""

    periodo: str
    total_huevos: int


class ProduccionReporte(BaseModel):
    """Resumen de producción del período."""

    total_huevos: int
    cubetas_completas: int
    huevos_no_aptos: int
    promedio_diario: float
    por_tipo: list[ProduccionTipoResponse]
    cubetas_por_tipo: list[CubetaTipoResponse]
    por_camada: list[ProduccionCamadaResponse]
    serie: list[SerieProduccionItem]


class ClienteVentaItem(BaseModel):
    """Cliente con el ingreso y número de pedidos del período."""

    id_cliente: int
    nombre_cliente: str
    ingreso_total: Decimal
    num_pedidos: int


class SerieVentaItem(BaseModel):
    """Punto de la serie de ventas para un período agrupado."""

    periodo: str
    ingreso_total: Decimal


class VentasReporte(BaseModel):
    """Resumen de ventas del período (sin pedidos cancelados)."""

    ingreso_total: Decimal
    unidades_totales: int
    num_pedidos: int
    ticket_promedio: Decimal
    por_tipo: list[ProduccionTipoResponse]
    top_clientes: list[ClienteVentaItem]
    serie: list[SerieVentaItem]


class InsumoConsumoItem(BaseModel):
    """Detalle de un insumo usado en el período.

    `costo_unitario` es el costo promedio ponderado vigente con el que se
    valorizó el consumo y `costo_total` = cantidad x costo_unitario.
    """

    id_insumo: int
    nombre_insumo: str
    categoria: str
    unidad_medida: str
    cantidad: Decimal
    costo_unitario: Decimal
    costo_total: Decimal


class GastosReporte(BaseModel):
    """Gasto del período: consumo de insumos valorizado al costo del stock."""

    total: Decimal
    por_insumo: list[InsumoConsumoItem]


class GananciaReporte(BaseModel):
    """Ganancia estimada del período: ventas menos gastos."""

    valor: Decimal
    porcentaje: Decimal


class SaludReporte(BaseModel):
    """Salud y bajas del período a partir de los eventos sanitarios."""

    gallinas_perdidas: int
    causa_principal: str | None
    vacunacion_al_dia: bool


class AlimentoReporte(BaseModel):
    """Consumo de insumos de categoría 'alimento' en el período."""

    kg_usados: Decimal
    promedio_diario: Decimal
    por_insumo: list[InsumoConsumoItem]


class InventarioHuevoItem(BaseModel):
    """Existencia de un tipo de huevo y su valor a precio de referencia."""

    id_tipo: int
    nombre_tipo: str
    cantidad_actual: int
    valor_unidad: Decimal
    valor_total: Decimal


class InventarioReporte(BaseModel):
    """Instantánea de inventario de huevos e insumos en alerta."""

    huevos: list[InventarioHuevoItem]
    valor_stock: Decimal
    insumos_bajo_umbral: int


class CamadaReporte(BaseModel):
    """Ficha de una camada específica incluida en el reporte.

    `aves_semana_28` se deriva de la cantidad inicial menos la mortalidad
    registrada hasta la semana 28 de vida. `promedio_produccion` es la
    postura media desde la semana 28 hasta hoy.
    """

    id_camada: int
    nombre_camada: str
    fecha_ingreso: date
    aves_semana_28: int
    edad_semanas: int
    edad_dias: int
    total_huevos: int
    cubetas_completas: int
    promedio_diario: float
    promedio_produccion: float


class ReporteConsolidadoResponse(BaseModel):
    """Reporte de rentabilidad consolidado por período (RF-37)."""

    periodo: PeriodoResponse
    produccion: ProduccionReporte
    ventas: VentasReporte
    gastos: GastosReporte
    ganancia: GananciaReporte
    salud: SaludReporte
    alimento: AlimentoReporte
    inventario: InventarioReporte
    camada: CamadaReporte | None = None

    model_config = ConfigDict(from_attributes=True)

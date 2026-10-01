from decimal import Decimal
from io import BytesIO

from app.schemas.reporte import ReporteConsolidadoResponse
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

_ESTILO_CABECERA = colors.HexColor("#905E27")
_ESTILO_FILA = colors.HexColor("#F7F3EA")


def generar_pdf(reporte: ReporteConsolidadoResponse) -> bytes:
    """Genera el PDF del reporte consolidado de rentabilidad (RF-38).

    Args:
        reporte: Consolidado calculado por `reporte_service`.

    Returns:
        bytes: El contenido binario del PDF listo para enviar.
    """
    buffer = BytesIO()
    documento = SimpleDocTemplate(
        buffer,
        pagesize=A4,
        title="EggChecker - Reporte de rentabilidad",
        author="EggChecker",
    )
    estilos = getSampleStyleSheet()
    elementos = [
        Paragraph("EggChecker — Reporte de rentabilidad", estilos["Title"]),
        Paragraph(_texto_periodo(reporte), estilos["Normal"]),
        Spacer(1, 0.5 * cm),
    ]

    if reporte.camada is not None:
        elementos += _seccion_camada(reporte, estilos)
    elementos += _seccion_cuentas(reporte, estilos)
    elementos += _seccion_produccion(reporte, estilos)
    elementos += _seccion_salud(reporte, estilos)
    elementos += _seccion_alimento(reporte, estilos)
    elementos += _seccion_inventario(reporte, estilos)

    documento.build(elementos)
    return buffer.getvalue()


def _texto_periodo(reporte: ReporteConsolidadoResponse) -> str:
    """Devuelve la línea descriptiva del período y la agrupación."""
    periodo = reporte.periodo
    return (
        f"Período: {periodo.desde.isoformat()} a {periodo.hasta.isoformat()} "
        f"· Agrupación: {periodo.agrupacion}"
    )


def _seccion_camada(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la ficha de la camada seleccionada."""
    camada = reporte.camada
    filas = [
        ["Camada", camada.nombre_camada],
        ["Fecha de ingreso", camada.fecha_ingreso.isoformat()],
        ["Aves a la semana 28", str(camada.aves_semana_28)],
        ["Edad actual", f"{camada.edad_semanas} semanas"],
        ["Promedio diario (período)", str(camada.promedio_diario)],
        ["Promedio en producción", str(camada.promedio_produccion)],
    ]
    return [
        Paragraph("Camada seleccionada", estilos["Heading2"]),
        _tabla(filas, anchos=[7 * cm, 8 * cm]),
        Spacer(1, 0.4 * cm),
    ]


def _seccion_cuentas(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la sección de cuentas: ventas, gastos y ganancia."""
    filas = [
        ["Concepto", "Valor"],
        ["Ventas", _moneda(reporte.ventas.ingreso_total)],
        ["Gastos", _moneda(reporte.gastos.total)],
        [
            "Ganancia",
            f"{_moneda(reporte.ganancia.valor)} " f"({reporte.ganancia.porcentaje}%)",
        ],
    ]
    return [
        Paragraph("Cuentas del período", estilos["Heading2"]),
        _tabla(filas, anchos=[7 * cm, 8 * cm]),
        Spacer(1, 0.4 * cm),
    ]


def _seccion_produccion(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la sección de producción del PDF."""
    produccion = reporte.produccion
    filas = [["Tipo de huevo", "Huevos", "Cubetas"]] + [
        [item.nombre_tipo, str(item.cantidad), str(_cubetas(produccion, item))]
        for item in produccion.por_tipo
    ]
    filas.append(
        [
            "Total",
            str(produccion.total_huevos),
            str(produccion.cubetas_completas),
        ]
    )
    return [
        Paragraph("Resumen de recogida", estilos["Heading2"]),
        Paragraph(
            f"Cubetas completas: <b>{produccion.cubetas_completas}</b> · "
            f"Promedio diario: {produccion.promedio_diario} · "
            f"Huevos no aptos: {produccion.huevos_no_aptos}",
            estilos["Normal"],
        ),
        Spacer(1, 0.2 * cm),
        _tabla(filas),
        Spacer(1, 0.4 * cm),
    ]


def _seccion_salud(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la sección de salud y bajas del PDF."""
    salud = reporte.salud
    filas = [
        ["Gallinas perdidas", str(salud.gallinas_perdidas)],
        ["Causa principal", salud.causa_principal or "Sin registro"],
        [
            "Vacunas y vitaminas",
            "Al día" if salud.vacunacion_al_dia else "Sin registro en el período",
        ],
    ]
    return [
        Paragraph("Salud y bajas", estilos["Heading2"]),
        _tabla(filas, anchos=[7 * cm, 8 * cm]),
        Spacer(1, 0.4 * cm),
    ]


def _seccion_alimento(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la sección de uso de comida del PDF."""
    alimento = reporte.alimento
    return [
        Paragraph("Uso de comida", estilos["Heading2"]),
        Paragraph(
            f"Se usaron <b>{alimento.kg_usados} kg</b> de alimento · "
            f"Promedio diario: {alimento.promedio_diario} kg",
            estilos["Normal"],
        ),
        Spacer(1, 0.4 * cm),
    ]


def _seccion_inventario(reporte: ReporteConsolidadoResponse, estilos) -> list:
    """Construye la sección de inventario del PDF."""
    inventario = reporte.inventario
    filas = [["Tipo de huevo", "Disponible", "Valor unidad", "Valor total"]] + [
        [
            item.nombre_tipo,
            str(item.cantidad_actual),
            _moneda(item.valor_unidad),
            _moneda(item.valor_total),
        ]
        for item in inventario.huevos
    ]
    filas.append(["Total", "", "", _moneda(inventario.valor_stock)])
    return [
        Paragraph("Inventario", estilos["Heading2"]),
        _tabla(filas, anchos=[5 * cm, 3 * cm, 4 * cm, 4 * cm]),
        Paragraph(
            f"Insumos bajo umbral: {inventario.insumos_bajo_umbral}",
            estilos["Normal"],
        ),
    ]


def _cubetas(produccion, item) -> int:
    """Devuelve las cubetas completas de un tipo de huevo."""
    for cubeta in produccion.cubetas_por_tipo:
        if cubeta.nombre_tipo == item.nombre_tipo:
            return cubeta.cubetas
    return 0


def _tabla(filas: list, anchos: list | None = None) -> Table:
    """Crea una tabla con el estilo visual del reporte."""
    tabla = Table(filas, colWidths=anchos, hAlign="LEFT")
    tabla.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), _ESTILO_CABECERA),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 9),
                ("BOTTOMPADDING", (0, 0), (-1, 0), 6),
                ("TOPPADDING", (0, 0), (-1, 0), 6),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, _ESTILO_FILA]),
                ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#D8CCB4")),
                ("ALIGN", (1, 1), (-1, -1), "RIGHT"),
            ]
        )
    )
    return tabla


def _moneda(valor: Decimal) -> str:
    """Formatea un valor monetario en pesos colombianos."""
    return f"${valor:,.2f}"

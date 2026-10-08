"""Recomendaciones personalizadas a partir del diagnóstico de IA (RF-35).

La IA describe lo que ve en la foto; este módulo cruza esas anomalías con
los datos de la granja (edad de la camada y postura de los últimos días)
para que el consejo sea concreto. Las reglas son deterministas y se
pueden revisar con el equipo y con un médico veterinario.
"""

from dataclasses import dataclass

from app.schemas.analisis_ia import DiagnosticoIA

# Máximo de recomendaciones que se muestran por análisis.
MAX_RECOMENDACIONES = 6

# Semanas a partir de las cuales la cáscara tiende a adelgazar.
SEMANAS_CASCARA_DELGADA = 60
# Las primeras semanas de postura producen huevos pequeños.
SEMANAS_INICIO_POSTURA = 30
# Postura (huevos/ave/día) por debajo de la cual conviene revisar el lote.
POSTURA_BAJA = 0.6

AVISO_VETERINARIO = (
    "Este diagnóstico es orientativo. Si las anomalías se repiten o aumentan, "
    "consulta a un médico veterinario o al técnico del ICA de tu zona."
)


@dataclass(frozen=True)
class ContextoGranja:
    """Datos de la granja que personalizan las recomendaciones."""

    nombre_camada: str | None = None
    edad_semanas: int | None = None
    aves_actuales: int | None = None
    postura_promedio: float | None = None


def _tipos(diagnostico: DiagnosticoIA) -> set[str]:
    """Tipos de anomalía detectados con confianza suficiente."""
    return {a.tipo for a in diagnostico.anomalias if a.confianza >= 0.4}


def _reglas(diagnostico: DiagnosticoIA, ctx: ContextoGranja) -> list[str]:
    """Aplica las reglas de manejo según anomalías y contexto."""
    tipos = _tipos(diagnostico)
    lote = f"la camada {ctx.nombre_camada}" if ctx.nombre_camada else "el lote"
    edad = ctx.edad_semanas
    recs: list[str] = []

    if tipos & {"grieta", "cascara_rota", "cascara_rugosa_delgada"}:
        if edad is not None and edad >= SEMANAS_CASCARA_DELGADA:
            recs.append(
                f"Las aves de {lote} tienen {edad} semanas: a esa edad la cáscara "
                "se adelgaza. Ofrece calcio en partícula gruesa (conchilla o "
                "carbonato de calcio) en la tarde y revisa que el alimento de "
                "postura tenga vitamina D3."
            )
        else:
            recs.append(
                f"Revisa el calcio del alimento de postura de {lote} y ofrece "
                "conchilla o carbonato de calcio en partícula gruesa; verifica "
                "que el agua esté limpia y siempre disponible."
            )
        recs.append(
            "Recoge los huevos al menos tres veces al día y revisa que los nidos "
            "tengan cama suficiente para que no se golpeen."
        )

    if "suciedad" in tipos:
        recs.append(
            "Cambia la cama de los nidos cuando esté húmeda o sucia y aumenta la "
            "frecuencia de recolección. Limpia los huevos en seco; si debes "
            "lavarlos, usa agua más tibia que el huevo y sécalos de inmediato."
        )

    if "mancha_sangre" in tipos:
        recs.append(
            "Las manchas de sangre suelen indicar estrés: evita ruidos, cambios "
            "bruscos de luz y manipulación de las aves. Separa esos huevos con "
            "el ovoscopio antes de vender."
        )

    if tipos & {"deformidad", "color_irregular"}:
        recs.append(
            "Huevos deformes o descoloridos pueden deberse a estrés, calor o "
            "enfermedades respiratorias como la bronquitis infecciosa. Revisa "
            "el plan de vacunación de " + lote + " y la ventilación del galpón."
        )

    if "tamano_anormal" in tipos:
        if edad is not None and edad < SEMANAS_INICIO_POSTURA:
            recs.append(
                f"Con {edad} semanas, {lote} está iniciando postura: es normal "
                "que algunos huevos sean pequeños o con doble yema. El tamaño se "
                "estabiliza en las próximas semanas."
            )
        else:
            recs.append(
                "Para un tamaño más uniforme, revisa la proteína del alimento y "
                "que todas las aves alcancen el comedero al mismo tiempo."
            )

    if (
        ctx.postura_promedio is not None
        and ctx.postura_promedio < POSTURA_BAJA
        and diagnostico.calidad_general != "buena"
    ):
        recs.append(
            f"La postura de {lote} está en {ctx.postura_promedio:.0%} en los "
            "últimos días. Junto con la calidad observada, conviene revisar "
            "alimento, agua, horas de luz y estado sanitario."
        )

    no_aptos = diagnostico.distribucion.No_apto
    if no_aptos > 0:
        plural = "huevos" if no_aptos > 1 else "huevo"
        recs.append(
            f"Separa {no_aptos} {plural} No apto antes de la venta y regístralos "
            "en Producción para que el inventario quede correcto."
        )

    if diagnostico.calidad_general == "buena" and not tipos:
        recs.append(
            f"La calidad de {lote} es buena. Mantén el manejo actual y repite el "
            "análisis cada semana para detectar cambios a tiempo."
        )

    return recs


def generar_recomendaciones(
    diagnostico: DiagnosticoIA, ctx: ContextoGranja
) -> list[str]:
    """Combina las reglas de la granja con las sugerencias de la IA.

    Primero van las recomendaciones personalizadas (reglas con datos de la
    camada), luego las de la IA que no repitan una regla, y al final el
    aviso de consulta veterinaria cuando hay anomalías.

    Args:
        diagnostico: Diagnóstico estructurado de la IA.
        ctx: Datos de la granja y de la camada analizada.

    Returns:
        list[str]: Recomendaciones sin duplicados, en orden de prioridad.
    """
    if not diagnostico.es_imagen_de_huevos:
        return [
            "Toma la foto con buena luz, de cerca y con los huevos sobre una "
            "superficie de color uniforme (por ejemplo, la bandeja o cubeta)."
        ]

    resultado: list[str] = []
    vistos: set[str] = set()
    for rec in _reglas(diagnostico, ctx) + diagnostico.recomendaciones:
        clave = rec.strip().lower()[:60]
        if rec.strip() and clave not in vistos:
            vistos.add(clave)
            resultado.append(rec.strip())

    resultado = resultado[: MAX_RECOMENDACIONES - 1]
    if diagnostico.anomalias:
        resultado.append(AVISO_VETERINARIO)
    return resultado

package com.adso.eggchecker.domain.model

/** Disponibilidad del análisis IA para el usuario (plan y cupo del día). */
data class EstadoIA(
    val disponible: Boolean,
    val plan: String,
    val modoDemo: Boolean,
    val proveedor: String,
    val limiteDiario: Int,
    val usadosHoy: Int,
    val mensaje: String
) {
    /** Análisis que aún puede hacer hoy. */
    val restantesHoy: Int get() = (limiteDiario - usadosHoy).coerceAtLeast(0)
}

/** Anomalía detectada en uno o varios huevos de la foto. */
data class Anomalia(
    val tipo: String,
    val descripcion: String,
    val gravedad: String,
    val huevosAfectados: Int,
    val confianza: Double
)

/** Clasificación estimada de los huevos según los tipos de la granja. */
data class DistribucionTipos(
    val aa: Int,
    val a: Int,
    val b: Int,
    val noApto: Int
) {
    val total: Int get() = aa + a + b + noApto
}

/** Análisis de calidad de huevos por fotografía (RF-33 a RF-36). */
data class Analisis(
    val idAnalisis: Int,
    val idCamada: Int?,
    val nombreCamada: String?,
    val fechaAnalisis: String,
    val tieneImagen: Boolean,
    val resultadoDiagnostico: String,
    val recomendaciones: List<String>,
    val calidadGeneral: String?,
    val puntajeCalidad: Int?,
    val aptoVenta: Boolean?,
    val huevosDetectados: Int?,
    val anomalias: List<Anomalia>,
    val distribucion: DistribucionTipos?,
    val confianzaGeneral: Double?,
    val proveedorIa: String?,
    val modoDemo: Boolean,
    val diagnosticoCorrecto: Boolean?
)

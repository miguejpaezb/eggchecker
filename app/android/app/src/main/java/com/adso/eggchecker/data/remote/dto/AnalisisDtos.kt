package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Respuesta de GET /analisis-ia/estado. */
data class EstadoIADto(
    @SerializedName("disponible") val disponible: Boolean,
    @SerializedName("plan") val plan: String,
    @SerializedName("modo_demo") val modoDemo: Boolean,
    @SerializedName("proveedor") val proveedor: String,
    @SerializedName("limite_diario") val limiteDiario: Int,
    @SerializedName("usados_hoy") val usadosHoy: Int,
    @SerializedName("mensaje") val mensaje: String
)

/** Anomalía detectada por la IA en la foto. */
data class AnomaliaDto(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("gravedad") val gravedad: String,
    @SerializedName("huevos_afectados") val huevosAfectados: Int,
    @SerializedName("confianza") val confianza: Double
)

/** Huevos estimados por tipo (AA, A, B, No apto). */
data class DistribucionDto(
    @SerializedName("AA") val aa: Int = 0,
    @SerializedName("A") val a: Int = 0,
    @SerializedName("B") val b: Int = 0,
    @SerializedName("No_apto") val noApto: Int = 0
)

/** Análisis guardado (POST /analisis-ia y listados). */
data class AnalisisDto(
    @SerializedName("id_analisis") val idAnalisis: Int,
    @SerializedName("id_camada") val idCamada: Int? = null,
    @SerializedName("nombre_camada") val nombreCamada: String? = null,
    @SerializedName("fecha_analisis") val fechaAnalisis: String,
    @SerializedName("tiene_imagen") val tieneImagen: Boolean = false,
    @SerializedName("resultado_diagnostico") val resultadoDiagnostico: String,
    @SerializedName("recomendaciones") val recomendaciones: List<String>? = null,
    @SerializedName("calidad_general") val calidadGeneral: String? = null,
    @SerializedName("puntaje_calidad") val puntajeCalidad: Int? = null,
    @SerializedName("apto_venta") val aptoVenta: Boolean? = null,
    @SerializedName("huevos_detectados") val huevosDetectados: Int? = null,
    @SerializedName("anomalias") val anomalias: List<AnomaliaDto>? = null,
    @SerializedName("distribucion") val distribucion: DistribucionDto? = null,
    @SerializedName("confianza_general") val confianzaGeneral: Double? = null,
    @SerializedName("proveedor_ia") val proveedorIa: String? = null,
    @SerializedName("modo_demo") val modoDemo: Boolean = false,
    @SerializedName("diagnostico_correcto") val diagnosticoCorrecto: Boolean? = null
)

/** Cuerpo de PATCH /analisis-ia/{id}/retroalimentacion. */
data class RetroalimentacionDto(
    @SerializedName("diagnostico_correcto") val diagnosticoCorrecto: Boolean
)

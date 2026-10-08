package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.AnalisisDto
import com.adso.eggchecker.data.remote.dto.AnomaliaDto
import com.adso.eggchecker.data.remote.dto.EstadoIADto
import com.adso.eggchecker.data.remote.dto.RetroalimentacionDto
import com.adso.eggchecker.domain.model.Analisis
import com.adso.eggchecker.domain.model.Anomalia
import com.adso.eggchecker.domain.model.DistribucionTipos
import com.adso.eggchecker.domain.model.EstadoIA

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Acceso al módulo de análisis de huevos con IA (RF-33 a RF-36). */
class AnalisisRepository(
    private val api: ApiService
) {

    /** Plan, modo demostración y cupo diario del usuario. */
    suspend fun estado(): Result<EstadoIA> =
        apiCall { api.obtenerEstadoIA() }.map { it.toDomain() }

    /**
     * Envía la foto al servidor para analizarla.
     *
     * @param fotoJpeg Foto ya comprimida en JPEG.
     * @param idCamada Camada de origen de los huevos (opcional).
     */
    suspend fun analizar(fotoJpeg: ByteArray, idCamada: Int?): Result<Analisis> =
        apiCall {
            val parteImagen = MultipartBody.Part.createFormData(
                name = "imagen",
                filename = "huevos.jpg",
                body = fotoJpeg.toRequestBody(TIPO_JPEG)
            )
            val parteCamada = idCamada?.toString()?.toRequestBody(TIPO_TEXTO)
            api.crearAnalisis(parteImagen, parteCamada)
        }.map { it.toDomain() }

    /** Historial de análisis, de toda la granja o de una camada. */
    suspend fun historial(idCamada: Int?): Result<List<Analisis>> =
        apiCall { api.listarAnalisis(idCamada) }
            .map { lista -> lista.map { it.toDomain() } }

    /** Bytes de la foto guardada de un análisis. */
    suspend fun imagen(idAnalisis: Int): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            apiCall { api.descargarImagenAnalisis(idAnalisis).bytes() }
        }

    /** Guarda si el diagnóstico fue correcto. */
    suspend fun retroalimentar(idAnalisis: Int, correcto: Boolean): Result<Analisis> =
        apiCall {
            api.retroalimentarAnalisis(idAnalisis, RetroalimentacionDto(correcto))
        }.map { it.toDomain() }

    /** Elimina el análisis y su foto. */
    suspend fun eliminar(idAnalisis: Int): Result<Unit> =
        apiCall { api.eliminarAnalisis(idAnalisis) }

    private companion object {
        val TIPO_JPEG = "image/jpeg".toMediaType()
        val TIPO_TEXTO = "text/plain".toMediaType()
    }
}

private fun EstadoIADto.toDomain(): EstadoIA = EstadoIA(
    disponible = disponible,
    plan = plan,
    modoDemo = modoDemo,
    proveedor = proveedor,
    limiteDiario = limiteDiario,
    usadosHoy = usadosHoy,
    mensaje = mensaje
)

private fun AnomaliaDto.toDomain(): Anomalia = Anomalia(
    tipo = tipo,
    descripcion = descripcion,
    gravedad = gravedad,
    huevosAfectados = huevosAfectados,
    confianza = confianza
)

private fun AnalisisDto.toDomain(): Analisis = Analisis(
    idAnalisis = idAnalisis,
    idCamada = idCamada,
    nombreCamada = nombreCamada,
    fechaAnalisis = fechaAnalisis,
    tieneImagen = tieneImagen,
    resultadoDiagnostico = resultadoDiagnostico,
    recomendaciones = recomendaciones.orEmpty(),
    calidadGeneral = calidadGeneral,
    puntajeCalidad = puntajeCalidad,
    aptoVenta = aptoVenta,
    huevosDetectados = huevosDetectados,
    anomalias = anomalias.orEmpty().map { it.toDomain() },
    distribucion = distribucion?.let {
        DistribucionTipos(aa = it.aa, a = it.a, b = it.b, noApto = it.noApto)
    },
    confianzaGeneral = confianzaGeneral,
    proveedorIa = proveedorIa,
    modoDemo = modoDemo,
    diagnosticoCorrecto = diagnosticoCorrecto
)

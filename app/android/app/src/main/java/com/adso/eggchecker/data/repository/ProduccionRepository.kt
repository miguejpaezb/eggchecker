package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.ProduccionConDetalleDto
import com.adso.eggchecker.data.remote.dto.ProduccionCreateDto
import com.adso.eggchecker.data.remote.dto.ProduccionDto
import com.adso.eggchecker.domain.model.Produccion
import com.adso.eggchecker.domain.model.ProduccionDetalle

/** Acceso a la producción diaria del usuario (modo online). */
class ProduccionRepository(
    private val api: ApiService
) {

    /** Lista la producción de una camada (más reciente primero). */
    suspend fun listar(idCamada: Int): Result<List<Produccion>> =
        apiCall { api.listarProduccion(idCamada, null) }
            .map { lista -> lista.map { it.toDomain() } }

    /** Obtiene una producción con su detalle por tipo. */
    suspend fun detalle(idProduccion: Int): Result<Produccion> =
        apiCall { api.obtenerProduccion(idProduccion) }.map { it.toDomain() }

    /** Registra o actualiza la recolección del día. */
    suspend fun registrar(
        idCamada: Int,
        fechaRecoleccion: String,
        aa: Int,
        a: Int,
        b: Int,
        noApto: Int
    ): Result<Produccion> = apiCall {
        api.registrarProduccion(
            ProduccionCreateDto(
                idCamada = idCamada,
                fechaRecoleccion = fechaRecoleccion,
                unidad = "unidad",
                aa = aa,
                a = a,
                b = b,
                noApto = noApto
            )
        )
    }.map { it.toDomain() }
}

private fun ProduccionDto.toDomain(): Produccion = Produccion(
    idProduccion = idProduccion,
    idCamada = idCamada,
    fechaRecoleccion = fechaRecoleccion,
    totalHuevos = totalHuevos,
    observaciones = observaciones,
    detalle = emptyList()
)

private fun ProduccionConDetalleDto.toDomain(): Produccion = Produccion(
    idProduccion = idProduccion,
    idCamada = idCamada,
    fechaRecoleccion = fechaRecoleccion,
    totalHuevos = totalHuevos,
    observaciones = observaciones,
    detalle = detalle.map {
        ProduccionDetalle(
            idTipo = it.idTipo,
            nombreTipo = it.nombreTipo,
            cantidad = it.cantidad
        )
    }
)

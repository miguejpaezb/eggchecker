package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.CamadaCreateDto
import com.adso.eggchecker.data.remote.dto.CamadaDto
import com.adso.eggchecker.data.remote.dto.CamadaUpdateDto
import com.adso.eggchecker.data.remote.dto.MortalidadRequestDto
import com.adso.eggchecker.domain.model.Camada

/** Acceso a las camadas del usuario autenticado (modo online). */
class CamadaRepository(
    private val api: ApiService
) {

    /** Lista las camadas, filtrando por estado si se indica. */
    suspend fun listar(estado: String?): Result<List<Camada>> =
        apiCall { api.listarCamadas(estado) }
            .map { lista -> lista.map { it.toDomain() } }

    /** Obtiene el detalle de una camada (incluye edad en días y retiro). */
    suspend fun detalle(idCamada: Int): Result<Camada> =
        apiCall { api.obtenerCamada(idCamada) }.map { it.toDomain() }

    /** Registra una camada nueva. */
    suspend fun crear(
        nombreCamada: String,
        fechaIngreso: String,
        cantidadInicial: Int,
        estado: String
    ): Result<Camada> = apiCall {
        api.crearCamada(
            CamadaCreateDto(
                nombreCamada = nombreCamada,
                fechaIngreso = fechaIngreso,
                cantidadInicial = cantidadInicial,
                estado = estado
            )
        )
    }.map { it.toDomain() }

    /** Edita el nombre y, si aplica, la cantidad inicial. */
    suspend fun editar(
        idCamada: Int,
        nombreCamada: String,
        cantidadInicial: Int?
    ): Result<Camada> = apiCall {
        api.actualizarCamada(
            idCamada,
            CamadaUpdateDto(
                nombreCamada = nombreCamada,
                cantidadInicial = cantidadInicial
            )
        )
    }.map { it.toDomain() }

    /** Registra mortalidad en una camada. */
    suspend fun mortalidad(idCamada: Int, cantidad: Int): Result<Camada> =
        apiCall { api.registrarMortalidad(idCamada, MortalidadRequestDto(cantidad)) }
            .map { it.toDomain() }

    /** Suma una semana de vida a la camada. */
    suspend fun avanzarSemana(idCamada: Int): Result<Camada> =
        apiCall { api.avanzarSemana(idCamada) }.map { it.toDomain() }

    /** Posponer la decisión de la camada una semana. */
    suspend fun seguirActiva(idCamada: Int): Result<Camada> =
        apiCall { api.seguirActiva(idCamada) }.map { it.toDomain() }

    /** Descarta la camada (estado retirada). */
    suspend fun descartar(idCamada: Int): Result<Camada> =
        apiCall { api.descartarCamada(idCamada) }.map { it.toDomain() }
}

private fun CamadaDto.toDomain(): Camada = Camada(
    idCamada = idCamada,
    idUsuario = idUsuario,
    nombreCamada = nombreCamada,
    fechaIngreso = fechaIngreso,
    cantidadInicial = cantidadInicial,
    cantidadActual = cantidadActual,
    estado = estado,
    edadSemanas = edadSemanas,
    fechaProximoAviso = fechaProximoAviso,
    fechaCreacion = fechaCreacion,
    requiereDecision = requiereDecision,
    puedeEditarInicial = puedeEditarInicial,
    edadDias = edadDias,
    fechaRetiroEstimada = fechaRetiroEstimada
)

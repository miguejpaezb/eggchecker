package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.DosisAplicadaDto
import com.adso.eggchecker.data.remote.dto.DosisAplicarDto
import com.adso.eggchecker.data.remote.dto.DosisDetalleDto
import com.adso.eggchecker.data.remote.dto.DosisDto
import com.adso.eggchecker.data.remote.dto.DosisLoteDto
import com.adso.eggchecker.data.remote.dto.RecordatorioCreateDto
import com.adso.eggchecker.data.remote.dto.RecordatorioDto
import com.adso.eggchecker.domain.model.Dosis
import com.adso.eggchecker.domain.model.DosisAplicada
import com.adso.eggchecker.domain.model.DosisDetalle
import com.adso.eggchecker.domain.model.Recordatorio

/** Acceso a los recordatorios sanitarios del usuario. */
class RecordatorioRepository(
    private val api: ApiService
) {

    /** Lista los recordatorios activos, opcionalmente de una camada. */
    suspend fun listar(
        idCamada: Int? = null,
        incluirGlobales: Boolean = true
    ): Result<List<Recordatorio>> = apiCall {
        api.listarRecordatorios(idCamada, incluirGlobales)
    }.map { lista -> lista.map { it.toDomain() } }

    /** Consulta un recordatorio con su próxima dosis. */
    suspend fun obtener(idRecordatorio: Int): Result<Recordatorio> = apiCall {
        api.obtenerRecordatorio(idRecordatorio)
    }.map { it.toDomain() }

    /** Crea una regla nueva (sin `idCamada` es global). */
    suspend fun crear(
        idCamada: Int?,
        idInsumo: Int,
        modo: String,
        intervaloDias: Int?,
        diasSemana: List<Int>?,
        fechas: List<String>?,
        fechaInicio: String?,
        fechaFin: String?,
        cantidad: Double,
        activo: Boolean
    ): Result<Recordatorio> = apiCall {
        api.crearRecordatorio(
            RecordatorioCreateDto(
                idCamada = idCamada,
                idInsumo = idInsumo,
                modo = modo,
                intervaloDias = intervaloDias,
                diasSemana = diasSemana,
                fechas = fechas,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                cantidad = cantidad,
                activo = activo
            )
        )
    }.map { it.toDomain() }

    /** Reemplaza una regla y recalcula sus dosis futuras. */
    suspend fun actualizar(
        idRecordatorio: Int,
        idCamada: Int?,
        idInsumo: Int,
        modo: String,
        intervaloDias: Int?,
        diasSemana: List<Int>?,
        fechas: List<String>?,
        fechaInicio: String?,
        fechaFin: String?,
        cantidad: Double,
        activo: Boolean
    ): Result<Recordatorio> = apiCall {
        api.actualizarRecordatorio(
            idRecordatorio,
            RecordatorioCreateDto(
                idCamada = idCamada,
                idInsumo = idInsumo,
                modo = modo,
                intervaloDias = intervaloDias,
                diasSemana = diasSemana,
                fechas = fechas,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                cantidad = cantidad,
                activo = activo
            )
        )
    }.map { it.toDomain() }

    /** Desactiva una regla y descarta sus dosis pendientes. */
    suspend fun eliminar(idRecordatorio: Int): Result<Unit> = apiCall {
        api.eliminarRecordatorio(idRecordatorio)
    }.map { }

    /** Dosis programadas de una regla, opcionalmente de un rango. */
    suspend fun dosis(
        idRecordatorio: Int,
        desde: String? = null,
        hasta: String? = null,
        soloPendientes: Boolean = false
    ): Result<List<Dosis>> = apiCall {
        api.listarDosis(idRecordatorio, desde, hasta, soloPendientes)
    }.map { lista -> lista.map { it.toDomain() } }

    /** Dosis con el insumo y el stock actual. */
    suspend fun dosisDetalle(idDosis: Int): Result<DosisDetalle> = apiCall {
        api.obtenerDosis(idDosis)
    }.map { it.toDomain() }

    /** Registra la aplicación de una dosis (descuenta inventario). */
    suspend fun aplicar(
        idDosis: Int,
        cantidad: Double? = null
    ): Result<DosisAplicada> = apiCall {
        api.aplicarDosis(idDosis, DosisAplicarDto(cantidad))
    }.map { it.toDomain() }

    /** Aplica varias dosis del mismo recordatorio en una transacción. */
    suspend fun aplicarLote(
        idsDosis: List<Int>,
        cantidad: Double? = null
    ): Result<List<DosisAplicada>> = apiCall {
        api.aplicarDosisLote(DosisLoteDto(idsDosis, cantidad))
    }.map { lista -> lista.map { it.toDomain() } }
}

private fun RecordatorioDto.toDomain(): Recordatorio = Recordatorio(
    idRecordatorio = idRecordatorio,
    idCamada = idCamada,
    nombreCamada = nombreCamada,
    idInsumo = idInsumo,
    nombreInsumo = nombreInsumo,
    unidadMedida = unidadMedida,
    stockActual = stockActual.toDoubleOrNull() ?: 0.0,
    cantidad = cantidad.toDoubleOrNull() ?: 0.0,
    modo = modo,
    intervaloDias = intervaloDias,
    diasSemana = diasSemana.orEmpty().sorted(),
    fechas = fechas.orEmpty(),
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    activo = activo,
    proximaDosis = proximaDosis,
    dosisPendientes = dosisPendientes
)

private fun DosisDto.toDomain(): Dosis = Dosis(
    idDosis = idDosis,
    idRecordatorio = idRecordatorio,
    idCamada = idCamada,
    nombreCamada = nombreCamada,
    fechaProgramada = fechaProgramada,
    numeroDosis = numeroDosis,
    aplicada = aplicada,
    fechaAplicacion = fechaAplicacion
)

private fun DosisDetalleDto.toDomain(): DosisDetalle = DosisDetalle(
    idDosis = idDosis,
    idRecordatorio = idRecordatorio,
    idCamada = idCamada,
    nombreCamada = nombreCamada,
    fechaProgramada = fechaProgramada,
    numeroDosis = numeroDosis,
    aplicada = aplicada,
    idInsumo = idInsumo,
    nombreInsumo = nombreInsumo,
    unidadMedida = unidadMedida,
    cantidad = cantidad.toDoubleOrNull() ?: 0.0,
    stockActual = stockActual.toDoubleOrNull() ?: 0.0
)

private fun DosisAplicadaDto.toDomain(): DosisAplicada = DosisAplicada(
    idDosis = idDosis,
    nombreCamada = nombreCamada,
    nombreInsumo = nombreInsumo,
    unidadMedida = unidadMedida,
    cantidad = cantidad.toDoubleOrNull() ?: 0.0,
    stockResultante = stockResultante.toDoubleOrNull() ?: 0.0
)

package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.InsumoCreateDto
import com.adso.eggchecker.data.remote.dto.InsumoDto
import com.adso.eggchecker.data.remote.dto.InsumoUpdateDto
import com.adso.eggchecker.data.remote.dto.MovimientoCreateDto
import com.adso.eggchecker.domain.model.Insumo

/** Acceso a los insumos del inventario del usuario. */
class InsumoRepository(
    private val api: ApiService
) {

    /**
     * Lista los insumos del usuario incluyendo los suspendidos.
     * El backend excluye siempre los descontinuados.
     */
    suspend fun listar(): Result<List<Insumo>> =
        apiCall { api.listarInsumos(categoria = null, activo = false) }
            .map { lista -> lista.map { it.toDomain() } }

    /** Registra un insumo nuevo. */
    suspend fun crear(
        idCategoria: Int,
        nombre: String,
        unidad: String,
        stockActual: Double,
        umbralMinimo: Double
    ): Result<Insumo> = apiCall {
        api.crearInsumo(
            InsumoCreateDto(
                idCategoria = idCategoria,
                nombreInsumo = nombre,
                unidadMedida = unidad,
                stockActual = stockActual,
                umbralMinimo = umbralMinimo
            )
        )
    }.map { it.toDomain() }

    /** Edita nombre, unidad y umbral de un insumo. */
    suspend fun editar(
        idInsumo: Int,
        nombre: String,
        unidad: String,
        umbralMinimo: Double
    ): Result<Insumo> = apiCall {
        api.actualizarInsumo(
            idInsumo,
            InsumoUpdateDto(
                nombreInsumo = nombre,
                unidadMedida = unidad,
                umbralMinimo = umbralMinimo
            )
        )
    }.map { it.toDomain() }

    /** Suma stock (movimiento de entrada). */
    suspend fun anadirStock(idInsumo: Int, cantidad: Double): Result<Unit> =
        apiCall {
            api.registrarMovimiento(
                idInsumo,
                MovimientoCreateDto(tipoMovimiento = "entrada", cantidad = cantidad)
            )
        }.map { }

    /** Suspende un insumo (reversible). */
    suspend fun suspender(idInsumo: Int): Result<Insumo> =
        apiCall { api.suspenderInsumo(idInsumo) }.map { it.toDomain() }

    /** Reactiva un insumo suspendido. */
    suspend fun activar(idInsumo: Int): Result<Insumo> =
        apiCall { api.activarInsumo(idInsumo) }.map { it.toDomain() }

    /** Descontinúa un insumo (permanente). */
    suspend fun descontinuar(idInsumo: Int): Result<Insumo> =
        apiCall { api.descontinuarInsumo(idInsumo) }.map { it.toDomain() }
}

private fun InsumoDto.toDomain(): Insumo = Insumo(
    idInsumo = idInsumo,
    idCategoria = idCategoria,
    nombreInsumo = nombreInsumo,
    unidadMedida = unidadMedida,
    stockActual = stockActual.toDoubleOrNull() ?: 0.0,
    umbralMinimo = umbralMinimo.toDoubleOrNull() ?: 0.0,
    costoUnitario = costoUnitario.toDoubleOrNull() ?: 0.0,
    activo = activo,
    descontinuado = descontinuado
)

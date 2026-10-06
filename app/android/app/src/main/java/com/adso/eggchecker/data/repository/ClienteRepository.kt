package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.ClienteCreateDto
import com.adso.eggchecker.data.remote.dto.ClienteDto
import com.adso.eggchecker.data.remote.dto.ClienteEliminarDto
import com.adso.eggchecker.data.remote.dto.ClienteUpdateDto
import com.adso.eggchecker.domain.model.Cliente

/** Acceso a los clientes del usuario (modo online). */
class ClienteRepository(
    private val api: ApiService
) {

    /** Lista los clientes incluyendo los suspendidos. */
    suspend fun listar(): Result<List<Cliente>> =
        apiCall { api.listarClientes(activo = false) }
            .map { lista -> lista.map { it.toDomain() } }

    /** Registra un cliente nuevo. */
    suspend fun crear(
        nombre: String,
        telefono: String?,
        direccion: String?
    ): Result<Cliente> = apiCall {
        api.crearCliente(
            ClienteCreateDto(
                nombreCliente = nombre,
                telefono = telefono,
                direccion = direccion
            )
        )
    }.map { it.toDomain() }

    /** Edita un cliente. */
    suspend fun editar(
        idCliente: Int,
        nombre: String,
        telefono: String?,
        direccion: String?
    ): Result<Cliente> = apiCall {
        api.actualizarCliente(
            idCliente,
            ClienteUpdateDto(
                nombreCliente = nombre,
                telefono = telefono,
                direccion = direccion
            )
        )
    }.map { it.toDomain() }

    /** Suspende un cliente (reversible). */
    suspend fun suspender(idCliente: Int): Result<Cliente> =
        apiCall { api.suspenderCliente(idCliente) }.map { it.toDomain() }

    /** Reactiva un cliente suspendido. */
    suspend fun activar(idCliente: Int): Result<Cliente> =
        apiCall { api.activarCliente(idCliente) }.map { it.toDomain() }

    /** Elimina un cliente confirmando la contraseña. */
    suspend fun eliminar(idCliente: Int, contrasena: String): Result<Unit> =
        apiCall {
            api.eliminarCliente(idCliente, ClienteEliminarDto(contrasena))
        }.map { }
}

private fun ClienteDto.toDomain(): Cliente = Cliente(
    idCliente = idCliente,
    nombreCliente = nombreCliente,
    telefono = telefono,
    direccion = direccion,
    fechaUltimaCompra = fechaUltimaCompra,
    activo = activo
)

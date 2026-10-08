package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Cliente del usuario devuelto por el backend. */
data class ClienteDto(
    @SerializedName("id_cliente") val idCliente: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_cliente") val nombreCliente: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("fecha_ultima_compra") val fechaUltimaCompra: String? = null,
    @SerializedName("activo") val activo: Boolean
)

/** Datos para POST /clientes. */
data class ClienteCreateDto(
    @SerializedName("nombre_cliente") val nombreCliente: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("direccion") val direccion: String? = null
)

/** Datos para PATCH /clientes/{id}. */
data class ClienteUpdateDto(
    @SerializedName("nombre_cliente") val nombreCliente: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("direccion") val direccion: String? = null
)

/** Datos para POST /clientes/{id}/eliminar. */
data class ClienteEliminarDto(
    @SerializedName("contrasena") val contrasena: String
)

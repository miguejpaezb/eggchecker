package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Credenciales enviadas a POST /auth/login. */
data class LoginRequestDto(
    @SerializedName("correo_electronico") val correoElectronico: String,
    @SerializedName("contrasena") val contrasena: String
)

/** Datos enviados a POST /auth/registro. */
data class RegistroRequestDto(
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("correo_electronico") val correoElectronico: String,
    @SerializedName("contrasena") val contrasena: String
)

/** Correo enviado a POST /auth/recuperar. */
data class RecuperarRequestDto(
    @SerializedName("correo_electronico") val correoElectronico: String
)

/** Respuesta de POST /auth/login con el token Bearer. */
data class TokenResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer"
)

/** Perfil devuelto por /auth/registro y GET /usuarios/me. */
data class UsuarioDto(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("correo_electronico") val correoElectronico: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("nombre_granja") val nombreGranja: String? = null,
    @SerializedName("plan_suscripcion") val planSuscripcion: String,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("activo") val activo: Boolean
)

/** Respuesta genérica con un mensaje para el usuario. */
data class MensajeResponseDto(
    @SerializedName("mensaje") val mensaje: String
)

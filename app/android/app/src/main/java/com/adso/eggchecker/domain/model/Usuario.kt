package com.adso.eggchecker.domain.model

/** Perfil del avicultor usado por la UI. */
data class Usuario(
    val idUsuario: Int,
    val nombreCompleto: String,
    val correoElectronico: String,
    val telefono: String?,
    val nombreGranja: String?,
    val planSuscripcion: String,
    val fechaRegistro: String,
    val activo: Boolean
)

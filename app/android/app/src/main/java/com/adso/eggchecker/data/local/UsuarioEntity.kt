package com.adso.eggchecker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Perfil del usuario guardado en SQLite con Room. */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val idUsuario: Int,
    val nombreCompleto: String,
    val correoElectronico: String,
    val telefono: String?,
    val nombreGranja: String?,
    val planSuscripcion: String,
    val fechaRegistro: String,
    val activo: Boolean
)

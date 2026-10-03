package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.local.UsuarioEntity
import com.adso.eggchecker.data.remote.dto.UsuarioDto
import com.adso.eggchecker.domain.model.Usuario

/** Convierte el DTO de la API en la entidad de Room. */
fun UsuarioDto.toEntity(): UsuarioEntity = UsuarioEntity(
    idUsuario = idUsuario,
    nombreCompleto = nombreCompleto,
    correoElectronico = correoElectronico,
    telefono = telefono,
    nombreGranja = nombreGranja,
    planSuscripcion = planSuscripcion,
    fechaRegistro = fechaRegistro,
    activo = activo
)

/** Convierte la entidad de Room en el modelo de dominio. */
fun UsuarioEntity.toDomain(): Usuario = Usuario(
    idUsuario = idUsuario,
    nombreCompleto = nombreCompleto,
    correoElectronico = correoElectronico,
    telefono = telefono,
    nombreGranja = nombreGranja,
    planSuscripcion = planSuscripcion,
    fechaRegistro = fechaRegistro,
    activo = activo
)

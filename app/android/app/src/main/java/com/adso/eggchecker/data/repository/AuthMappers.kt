package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.local.UsuarioEntity
import com.adso.eggchecker.data.remote.dto.PerfilDto
import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.domain.model.Usuario

/** Convierte el perfil de la API en la entidad de Room (solo datos base). */
fun PerfilDto.toEntity(): UsuarioEntity = UsuarioEntity(
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

/** Convierte el perfil de la API en el modelo de la pantalla de Perfil. */
fun PerfilDto.toPerfil(): Perfil = Perfil(
    idUsuario = idUsuario,
    nombreCompleto = nombreCompleto,
    correoElectronico = correoElectronico,
    telefono = telefono,
    nombreGranja = nombreGranja,
    planSuscripcion = planSuscripcion,
    fechaRegistro = fechaRegistro,
    activo = activo,
    avesMax = avesMax,
    clientesMax = clientesMax,
    iaIncluida = iaIncluida,
    avesActuales = avesActuales,
    clientesActuales = clientesActuales,
    totalHuevosProducidos = totalHuevosProducidos,
    totalAvesGestionadas = totalAvesGestionadas,
    notifProduccionBaja = notifProduccionBaja,
    notifStockBajo = notifStockBajo,
    notifVacunacion = notifVacunacion,
    notifResumenSemanal = notifResumenSemanal
)

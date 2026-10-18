package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Perfil completo del usuario devuelto por GET /usuarios/me. */
data class PerfilDto(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("correo_electronico") val correoElectronico: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("nombre_granja") val nombreGranja: String? = null,
    @SerializedName("plan_suscripcion") val planSuscripcion: String,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("plan") val plan: String,
    @SerializedName("aves_max") val avesMax: Int? = null,
    @SerializedName("clientes_max") val clientesMax: Int? = null,
    @SerializedName("ia_incluida") val iaIncluida: Boolean = false,
    @SerializedName("aves_actuales") val avesActuales: Int = 0,
    @SerializedName("clientes_actuales") val clientesActuales: Int = 0,
    @SerializedName("total_huevos_producidos") val totalHuevosProducidos: Int = 0,
    @SerializedName("total_aves_gestionadas") val totalAvesGestionadas: Int = 0,
    @SerializedName("notif_produccion_baja") val notifProduccionBaja: Boolean = true,
    @SerializedName("notif_camada_limite") val notifCamadaLimite: Boolean = true,
    @SerializedName("notif_produccion_hora") val notifProduccionHora: String = "05:00:00",
    @SerializedName("notif_stock_bajo") val notifStockBajo: Boolean = true,
    @SerializedName("notif_vacunacion") val notifVacunacion: Boolean = false,
    @SerializedName("notif_resumen_semanal") val notifResumenSemanal: Boolean = true,
    @SerializedName("notif_resumen_frecuencia") val notifResumenFrecuencia: String = "semanal",
    @SerializedName("notif_resumen_dia") val notifResumenDia: Int = 0
)

/** Datos editables del perfil (PUT /usuarios/me). */
data class PerfilUpdateDto(
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("correo_electronico") val correoElectronico: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("nombre_granja") val nombreGranja: String? = null,
    @SerializedName("contrasena_actual") val contrasenaActual: String? = null
)

/** Preferencias de alertas (PUT /usuarios/me/notificaciones).
 *
 * Los campos con valor nulo no se envían (Gson omite los nulos) y el
 * servidor conserva el valor guardado.
 */
data class NotificacionesUpdateDto(
    @SerializedName("notif_produccion_baja") val notifProduccionBaja: Boolean,
    @SerializedName("notif_stock_bajo") val notifStockBajo: Boolean,
    @SerializedName("notif_vacunacion") val notifVacunacion: Boolean,
    @SerializedName("notif_resumen_semanal") val notifResumenSemanal: Boolean,
    @SerializedName("notif_camada_limite") val notifCamadaLimite: Boolean? = null,
    @SerializedName("notif_produccion_hora") val notifProduccionHora: String? = null,
    @SerializedName("notif_resumen_frecuencia") val notifResumenFrecuencia: String? = null,
    @SerializedName("notif_resumen_dia") val notifResumenDia: Int? = null
)

/** Datos para cambiar la contraseña (PUT /usuarios/me/contrasena). */
data class CambiarContrasenaDto(
    @SerializedName("contrasena_actual") val contrasenaActual: String,
    @SerializedName("contrasena_nueva") val contrasenaNueva: String
)

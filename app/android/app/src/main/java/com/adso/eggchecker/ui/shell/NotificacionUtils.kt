package com.adso.eggchecker.ui.shell

import com.adso.eggchecker.domain.model.Perfil

/**
 * Decide si un aviso debe publicarse en el dispositivo según las
 * preferencias del perfil. Es la red de seguridad del cliente: el backend
 * ya no genera avisos de tipos apagados.
 */
fun debeMostrarNotificacion(tipo: String, perfil: Perfil?): Boolean {
    if (perfil == null) return true
    return when (tipo) {
        "stock_minimo", "stock_bajo", "sin_stock" -> perfil.notifStockBajo
        "camada_limite" -> perfil.notifCamadaLimite
        "produccion_baja", "produccion_sin_registro" -> perfil.notifProduccionBaja
        "recordatorio_sanitario" -> perfil.notifVacunacion
        "resumen_periodico" -> perfil.notifResumenSemanal
        else -> true
    }
}

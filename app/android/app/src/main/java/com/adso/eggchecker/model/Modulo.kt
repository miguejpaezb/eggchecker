package com.adso.eggchecker.model

import androidx.annotation.DrawableRes

import com.adso.eggchecker.R
import com.adso.eggchecker.navigation.Rutas

/** Un ítem del menú lateral de EggChecker. */
data class Modulo(
    val ruta: String,
    val nombre: String,
    @DrawableRes val icono: Int
)

/** Módulos del drawer, en el mismo orden que la versión web. */
val MODULOS = listOf(
    Modulo(Rutas.DASHBOARD, "Dashboard", R.drawable.ic_dashboard),
    Modulo(Rutas.PRODUCCION, "Producción", R.drawable.ic_produccion),
    Modulo(Rutas.CAMADAS, "Camadas", R.drawable.ic_camadas),
    Modulo(Rutas.INVENTARIO, "Inventario", R.drawable.ic_inventario),
    Modulo(Rutas.CLIENTES, "Clientes", R.drawable.ic_clientes),
    Modulo(Rutas.VENTAS, "Ventas", R.drawable.ic_ventas),
    Modulo(Rutas.ANALISIS, "Análisis IA", R.drawable.ic_analisis),
    Modulo(Rutas.REPORTES, "Reportes", R.drawable.ic_reportes),
    Modulo(Rutas.PERFIL, "Perfil", R.drawable.ic_perfil)
)

package com.adso.eggchecker.domain.model

/** Categoría del catálogo global de insumos. */
data class Categoria(
    val idCategoria: Int,
    val nombreCateg: String,
    val descripcion: String?
)

/** Insumo del inventario del usuario. */
data class Insumo(
    val idInsumo: Int,
    val idCategoria: Int,
    val nombreInsumo: String,
    val unidadMedida: String,
    val stockActual: Double,
    val umbralMinimo: Double,
    val costoUnitario: Double,
    val activo: Boolean,
    val descontinuado: Boolean
)

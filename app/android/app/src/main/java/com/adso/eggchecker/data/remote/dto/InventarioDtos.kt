package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Categoría del catálogo global de insumos. */
data class CategoriaDto(
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("nombre_categ") val nombreCateg: String,
    @SerializedName("descripcion") val descripcion: String? = null
)

/** Datos para POST /categorias-insumo. */
data class CategoriaCreateDto(
    @SerializedName("nombre_categ") val nombreCateg: String,
    @SerializedName("descripcion") val descripcion: String? = null
)

/** Datos para PATCH /categorias-insumo/{id}. */
data class CategoriaUpdateDto(
    @SerializedName("nombre_categ") val nombreCateg: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null
)

/** Insumo del inventario del usuario. Los decimales llegan como texto. */
data class InsumoDto(
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("stock_actual") val stockActual: String,
    @SerializedName("umbral_minimo") val umbralMinimo: String,
    @SerializedName("costo_unitario") val costoUnitario: String,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("descontinuado") val descontinuado: Boolean
)

/** Datos para POST /insumos. */
data class InsumoCreateDto(
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("stock_actual") val stockActual: Double,
    @SerializedName("umbral_minimo") val umbralMinimo: Double
)

/** Datos para PATCH /insumos/{id}. Los nulos se omiten en el JSON. */
data class InsumoUpdateDto(
    @SerializedName("nombre_insumo") val nombreInsumo: String? = null,
    @SerializedName("unidad_medida") val unidadMedida: String? = null,
    @SerializedName("umbral_minimo") val umbralMinimo: Double? = null
)

/** Datos para POST /insumos/{id}/movimientos. */
data class MovimientoCreateDto(
    @SerializedName("tipo_movimiento") val tipoMovimiento: String,
    @SerializedName("cantidad") val cantidad: Double
)

/** Movimiento de stock devuelto por el backend. */
data class MovimientoDto(
    @SerializedName("id_movimiento") val idMovimiento: Int,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("tipo_movimiento") val tipoMovimiento: String,
    @SerializedName("cantidad") val cantidad: String,
    @SerializedName("costo_unitario") val costoUnitario: String? = null,
    @SerializedName("fecha_movimiento") val fechaMovimiento: String,
    @SerializedName("observaciones") val observaciones: String? = null,
    @SerializedName("stock_resultante") val stockResultante: String
)

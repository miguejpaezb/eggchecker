package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.CategoriaCreateDto
import com.adso.eggchecker.data.remote.dto.CategoriaDto
import com.adso.eggchecker.data.remote.dto.CategoriaUpdateDto
import com.adso.eggchecker.domain.model.Categoria

/** Acceso al catálogo global de categorías de insumo. */
class CategoriaRepository(
    private val api: ApiService
) {

    /** Lista las categorías ordenadas por nombre. */
    suspend fun listar(): Result<List<Categoria>> =
        apiCall { api.listarCategorias() }
            .map { lista -> lista.map { it.toDomain() } }

    /** Crea una categoría nueva. */
    suspend fun crear(nombre: String, descripcion: String?): Result<Categoria> =
        apiCall {
            api.crearCategoria(
                CategoriaCreateDto(nombreCateg = nombre, descripcion = descripcion)
            )
        }.map { it.toDomain() }

    /** Edita una categoría existente. */
    suspend fun actualizar(
        idCategoria: Int,
        nombre: String,
        descripcion: String?
    ): Result<Categoria> = apiCall {
        api.actualizarCategoria(
            idCategoria,
            CategoriaUpdateDto(nombreCateg = nombre, descripcion = descripcion)
        )
    }.map { it.toDomain() }

    /** Elimina una categoría sin insumos asociados. */
    suspend fun eliminar(idCategoria: Int): Result<Unit> =
        apiCall { api.eliminarCategoria(idCategoria) }.map { }
}

private fun CategoriaDto.toDomain(): Categoria = Categoria(
    idCategoria = idCategoria,
    nombreCateg = nombreCateg,
    descripcion = descripcion
)

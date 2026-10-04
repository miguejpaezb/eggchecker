package com.adso.eggchecker.data.remote

import com.google.gson.JsonParser

import retrofit2.HttpException
import java.io.IOException

/** Error de la API con un mensaje listo para mostrar al usuario. */
class ApiException(
    val codigo: Int?,
    message: String
) : Exception(message)

/**
 * Ejecuta una llamada a la API y normaliza los errores.
 *
 * FastAPI devuelve `detail` como texto o como lista de errores de validación.
 */
suspend fun <T> apiCall(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: HttpException) {
    Result.failure(ApiException(e.code(), extraerDetalle(e)))
} catch (e: IOException) {
    Result.failure(
        ApiException(null, "No se pudo conectar con el servidor")
    )
} catch (e: Exception) {
    Result.failure(ApiException(null, "Ocurrió un error inesperado"))
}

/** Extrae el mensaje `detail` del cuerpo de error de FastAPI. */
private fun extraerDetalle(e: HttpException): String {
    val cuerpo = e.response()?.errorBody()?.string()
    if (cuerpo.isNullOrBlank()) {
        return "Ocurrió un error inesperado"
    }
    return try {
        val detail = JsonParser.parseString(cuerpo).asJsonObject.get("detail")
        when {
            detail == null -> "Ocurrió un error inesperado"
            detail.isJsonPrimitive -> detail.asString
            detail.isJsonArray -> detail.asJsonArray.joinToString(". ") { item ->
                item.asJsonObject.get("msg")?.asString ?: item.toString()
            }
            else -> "Ocurrió un error inesperado"
        }
    } catch (e: Exception) {
        "Ocurrió un error inesperado"
    }
}

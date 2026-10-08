package com.adso.eggchecker.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

import androidx.core.content.FileProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Prepara las fotos del análisis IA (RF-33).
 *
 * - Crea el archivo temporal donde la cámara guarda la foto.
 * - Lee la foto (cámara o galería), corrige la rotación EXIF y la reduce a
 *   [LADO_MAXIMO] px en JPEG. Así se envían unos 200–400 KB en lugar de
 *   varios MB: el análisis es más rápido y gasta menos datos móviles.
 */
class FotoAnalisisStorage(
    private val context: Context
) {

    /** Uri temporal (FileProvider) para `ActivityResultContracts.TakePicture`. */
    fun crearUriCamara(): Uri {
        val carpeta = File(context.cacheDir, CARPETA).apply { mkdirs() }
        val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    /**
     * Lee la foto y la devuelve comprimida en JPEG.
     *
     * @param uri Foto tomada con la cámara o elegida en la galería.
     * @return Bytes JPEG listos para enviar al servidor.
     */
    suspend fun leerComprimida(uri: Uri): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver

                val limites = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, limites)
                }
                require(limites.outWidth > 0 && limites.outHeight > 0) {
                    "No se pudo leer la foto"
                }

                val opciones = BitmapFactory.Options().apply {
                    inSampleSize = calcularMuestreo(
                        limites.outWidth,
                        limites.outHeight
                    )
                }
                val original = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opciones)
                } ?: error("No se pudo leer la foto")

                // Si la foto no trae EXIF legible, se usa sin rotar.
                val rotacion = runCatching {
                    resolver.openInputStream(uri)?.use { gradosExif(ExifInterface(it)) }
                }.getOrNull() ?: 0

                val procesada = escalarYRotar(original, rotacion)
                ByteArrayOutputStream().use { salida ->
                    procesada.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
                    if (procesada !== original) procesada.recycle()
                    original.recycle()
                    salida.toByteArray()
                }
            }
        }

    /** Borra las fotos temporales de la cámara. */
    fun limpiarTemporales() {
        File(context.cacheDir, CARPETA).listFiles()?.forEach { it.delete() }
    }

    private fun calcularMuestreo(ancho: Int, alto: Int): Int {
        var muestreo = 1
        while (maxOf(ancho, alto) / (muestreo * 2) >= LADO_MAXIMO) {
            muestreo *= 2
        }
        return muestreo
    }

    private fun gradosExif(exif: ExifInterface): Int =
        when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun escalarYRotar(bitmap: Bitmap, grados: Int): Bitmap {
        val ladoMayor = maxOf(bitmap.width, bitmap.height)
        val escala = if (ladoMayor > LADO_MAXIMO) {
            LADO_MAXIMO.toFloat() / ladoMayor
        } else {
            1f
        }
        if (escala == 1f && grados == 0) return bitmap
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(grados.toFloat())
        }
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true
        )
    }

    private companion object {
        const val CARPETA = "analisis"
        const val LADO_MAXIMO = 1280
        const val CALIDAD_JPEG = 85
    }
}

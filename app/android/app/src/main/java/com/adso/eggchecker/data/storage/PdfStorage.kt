package com.adso.eggchecker.data.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

/** Guarda archivos PDF en la carpeta pública de Descargas del dispositivo. */
class PdfStorage(
    private val context: Context
) {

    /**
     * Guarda los bytes de un PDF en Descargas/EggChecker.
     *
     * En Android 10+ usa MediaStore (sin permiso); en versiones anteriores
     * escribe directamente en el almacenamiento externo público.
     *
     * @param nombre Nombre del archivo con extensión .pdf.
     * @param bytes Contenido binario del PDF.
     * @return Result con éxito si el archivo quedó guardado.
     */
    suspend fun guardar(nombre: String, bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    guardarConMediaStore(nombre, bytes)
                } else {
                    guardarEnDescargas(nombre, bytes)
                }
            }
        }

    private fun guardarConMediaStore(nombre: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nombre)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/EggChecker"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            valores
        ) ?: error("No se pudo crear el archivo en Descargas")

        resolver.openOutputStream(uri)?.use { salida ->
            salida.write(bytes)
        } ?: error("No se pudo escribir el archivo en Descargas")

        valores.clear()
        valores.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, valores, null, null)
    }

    @Suppress("DEPRECATION")
    private fun guardarEnDescargas(nombre: String, bytes: ByteArray) {
        val directorio = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            "EggChecker"
        )
        if (!directorio.exists() && !directorio.mkdirs()) {
            error("No se pudo crear la carpeta de Descargas")
        }
        val archivo = File(directorio, nombre)
        archivo.writeBytes(bytes)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(archivo.absolutePath),
            arrayOf("application/pdf"),
            null
        )
    }
}

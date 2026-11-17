package com.cronos

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/**
 * Puente que usa la página (window.CronosFiles) para leer y escribir las hojas
 * de cálculo de cada temporada en la carpeta pública Descargas/Cronos del móvil,
 * donde se pueden abrir con Excel, Google Sheets o compartir.
 *
 * Los archivos viajan en Base64 porque el puente JavaScript solo pasa texto.
 */
class SeasonFiles(private val activity: Activity) {

    private val folder = "Cronos"
    private val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Carpeta que se muestra al usuario. */
    @JavascriptInterface
    fun location(): String = "Descargas/$folder"

    /** Devuelve el archivo en Base64, o "" si todavía no existe. */
    @JavascriptInterface
    fun read(name: String): String {
        return try {
            val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findQ(name) ?: return ""
                activity.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
            } else {
                val f = legacyFile(name)
                if (!f.exists()) return ""
                f.readBytes()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Crea el archivo si no existe o lo sustituye por la versión nueva.
     * Devuelve "ok", "perm" (falta el permiso en Android 9 o anterior) o "error:...".
     */
    @JavascriptInterface
    fun write(name: String, base64: String): String {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeQ(name, bytes)
            } else {
                if (!hasLegacyPermission()) {
                    activity.runOnUiThread {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 7
                        )
                    }
                    return "perm"
                }
                val f = legacyFile(name)
                f.parentFile?.mkdirs()
                FileOutputStream(f, false).use { it.write(bytes) }
                MediaScannerConnection.scanFile(activity, arrayOf(f.absolutePath), arrayOf(mime), null)
            }
            "ok"
        } catch (e: Exception) {
            "error:" + (e.message ?: e.javaClass.simpleName)
        }
    }

    // ---- Android 10 y posteriores: MediaStore, sin permisos ----

    private val relPath get() = Environment.DIRECTORY_DOWNLOADS + "/" + folder + "/"

    private fun findQ(name: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val sel = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " +
            MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        activity.contentResolver.query(collection, projection, sel, arrayOf(name, relPath), null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    return Uri.withAppendedPath(collection, c.getLong(0).toString())
                }
            }
        return null
    }

    private fun writeQ(name: String, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val resolver = activity.contentResolver
        val existing = findQ(name)
        if (existing != null) {
            resolver.openFileDescriptor(existing, "rwt")!!.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { out ->
                    out.channel.truncate(0)
                    out.write(bytes)
                }
            }
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("No se pudo crear el archivo")
        resolver.openOutputStream(uri)!!.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    // ---- Android 9 y anteriores: archivo directo con permiso de almacenamiento ----

    @Suppress("DEPRECATION")
    private fun legacyFile(name: String): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$folder/$name")

    private fun hasLegacyPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
}

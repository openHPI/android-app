package de.xikolo.utils.extensions

import android.content.Context
import android.os.Environment
import android.util.Log
import de.xikolo.App
import de.xikolo.R
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileExtensions {
    val TAG: String = FileExtensions::class.java.simpleName
}

val <T : File?> T.fileSize: Long
    get() {
        var length: Long = 0
        if (this != null && isFile) {
            length = length()
        }
        return length
    }

val Long.asFormattedFileSize: String
    get() {
        if (this <= 0)
            return "0 MB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

val <T : File?> T.fileCount: Int
    get() {
        var files = 0
        if (this != null && listFiles() != null)
            for (file in listFiles()) {
                if (file.isFile) {
                    files++
                } else {
                    files += file.fileCount
                }
            }
        return files
    }

fun <T : File> T.createIfNotExists() {
    var folder: File = this
    if (!exists()) {
        if (isFile) {
            folder = folder.parentFile
        }

        Log.d(FileExtensions.TAG, "Folder " + folder.absolutePath + " not exists")
        if (folder.mkdirs()) {
            Log.d(FileExtensions.TAG, "Created Folder " + folder.absolutePath)
        } else {
            Log.w(FileExtensions.TAG, "Failed creating Folder " + folder.absolutePath)
        }
    } else {
        Log.d(FileExtensions.TAG, "Folder " + folder.absolutePath + " already exists")
    }
}

val String.asEscapedFileName: String
    get() {
        // Source http://gordon.koefner.at/blog/coding/replacing-german-umlauts
        val input = this
        //replace all lower Umlauts
        var output = input.replace("ü", "ue")
            .replace("ö", "oe")
            .replace("ä", "ae")
            .replace("ß", "ss")

        //first replace all capital umlaute in a non-capitalized context (e.g. Übung)
        output = output.replace("Ü(?=[a-zäöüß ])", "Ue")
            .replace("Ö(?=[a-zäöüß ])", "Oe")
            .replace("Ä(?=[a-zäöüß ])", "Ae")

        //now replace all the other capital umlaute
        output = output.replace("Ü", "UE")
            .replace("Ö", "OE")
            .replace("Ä", "AE")

        return output.replace("[^a-zA-Z0-9\\(\\).-]".toRegex(), "_")
    }

val <T : Context> T.publicAppStorageFolder: File
    get() {
        return File(Environment.getExternalStorageDirectory().absolutePath + File.separator
            + App.instance.getString(R.string.app_name))
    }

package de.xikolo.models

import android.os.Environment
import java.io.File

data class Storage(val file: File) {

    enum class Type {
        INTERNAL, SDCARD
    }

    val isWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState(file)
            return state == Environment.MEDIA_MOUNTED
        }
}

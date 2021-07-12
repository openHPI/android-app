package de.xikolo.utils.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import de.xikolo.App
import de.xikolo.R
import de.xikolo.models.Storage
import de.xikolo.storages.ApplicationPreferences


val <T : Context> T.storages: Array<Storage>
    get() {
        val storageList = mutableListOf<Storage>()
        ContextCompat.getExternalFilesDirs(this, null).forEach {
            if (it != null) {
                storageList.add(Storage(it))
            }
        }
        return storageList.toTypedArray()
    }

/* Returns the "internal" storage. */
val <T : Context> T.internalStorage: Storage
    get() {
        return storages[0]
    }

/* Returns the "SD Card" storage or null if not available. */
val <T : Context> T.sdcardStorage: Storage?
    get() {
        return if (storages.size > 1) storages[1] else null
    }

val String.asStorageType: Storage.Type
    get() {
        return if (this == App.instance.getString(R.string.settings_title_storage_external))
            Storage.Type.SDCARD
        else
            Storage.Type.INTERNAL
    }

val <T : Context> T.preferredStorage: Storage
    get() {
        return when (storagePreference) {
            Storage.Type.SDCARD -> {
                sdcardStorage ?: internalStorage
            }
            else                -> internalStorage
        }
    }

fun <T : Context> T.buildWriteErrorMessage(): String {
    var storage = getString(R.string.settings_title_storage_internal)
    if (storagePreference == Storage.Type.SDCARD)
        storage = getString(R.string.settings_title_storage_external)
    return getString(R.string.toast_no_external_write_access, storage)
}

private val storagePreference: Storage.Type
    get() {
        return ApplicationPreferences().storage.asStorageType
    }

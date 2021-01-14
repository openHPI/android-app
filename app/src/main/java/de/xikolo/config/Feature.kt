package de.xikolo.config

import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.AnyRes
import de.xikolo.App
import de.xikolo.utils.extensions.getBoolean
import de.xikolo.utils.extensions.getResId
import de.xikolo.utils.extensions.getStringArray
import de.xikolo.utils.extensions.resExists

object Feature {

    @JvmField
    val PIP =
        SDK_INT >= 26 && App.instance.packageManager
            .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    val SHORTCUTS = SDK_INT >= 25

    fun enabled(name: String): Boolean {
        val context = App.instance

        val strategies = mapOf<String, (@AnyRes Int) -> Boolean>(
            "string" to { id -> context.getString(id).isNotBlank() },
            "bool" to { id -> context.getBoolean(id) },
            "array" to { id -> context.getStringArray(id).isNotEmpty() }
        )

        return strategies.any { (type, strategy) ->
            context.resExists(name, type) && strategy.invoke(context.getResId(name, type))
        }
    }
}

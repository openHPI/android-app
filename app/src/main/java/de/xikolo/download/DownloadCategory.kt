package de.xikolo.download

import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Represents a download category.
 * This class is sealed to ensure exhaustiveness.
 */
@JsonAdapter(DownloadCategory.Companion.JsonAdapter::class)
sealed class DownloadCategory {

    /**
     * Category other downloads.
     */
    object Other : DownloadCategory()

    /**
     * Documents category.
     */
    object Documents : DownloadCategory()

    /**
     * Certificates category.
     */
    object Certificates : DownloadCategory()

    /**
     * Course category.
     *
     * @param id The course id.
     */
    data class Course(val id: String) : DownloadCategory()

    companion object {

        /**
         * Custom JSON adapter to persist the class names.
         */
        class JsonAdapter : TypeAdapter<DownloadCategory>() {
            override fun write(out: JsonWriter?, value: DownloadCategory?) {
                out?.beginObject()
                out?.name("name")
                when (value) {
                    is Other -> out?.value("other")
                    is Documents -> out?.value("documents")
                    is Certificates -> out?.value("certificates")
                    is Course -> {
                        out?.value("course")
                        out?.name("id")
                        out?.value(value.id)
                    }
                }
                out?.endObject()
            }

            override fun read(input: JsonReader?): DownloadCategory? {
                input?.beginObject()
                input?.nextName()
                val category = when (input?.nextString()) {
                    "other" -> Other
                    "documents" -> Documents
                    "certificates" -> Certificates
                    "course" -> {
                        input.nextName()
                        Course(input.nextString())
                    }
                    else -> throw JsonSyntaxException("unsupported category")
                }
                input.endObject()
                return category
            }
        }
    }
}

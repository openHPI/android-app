import org.gradle.api.JavaVersion

object Versions {
    // Project
    const val KOTLIN = "1.4.10"
    const val BUILD_TOOLS = "29.0.3"
    const val MIN_SDK = 21
    const val TARGET_SDK = 29
    const val COMPILE_SDK = 29

    const val VERSION_NAME = "3.4"
    const val VERSION_CODE = 58

    const val XIKOLO_API = 4
    const val REALM_SCHEMA = 13

    val JAVA = JavaVersion.VERSION_1_8
    const val KOTLIN_JVM = "1.8"

    // Dependencies
    const val OK_HTTP = "4.8.1"
    const val GSON = "2.8.6"
}

object SharedDependencies {
    const val OK_HTTP = "com.squareup.okhttp3:okhttp:${Versions.OK_HTTP}"
    const val GSON = "com.google.code.gson:gson:${Versions.GSON}"
}

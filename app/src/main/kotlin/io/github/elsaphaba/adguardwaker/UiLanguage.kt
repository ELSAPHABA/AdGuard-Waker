package io.github.elsaphaba.adguardwaker

/** Explicit user choices take priority; otherwise only Korean systems use Korean UI. */
internal object UiLanguage {
    const val KEY = "ui_language"
    const val AUTO = "auto"
    const val KOREAN = "ko"
    const val ENGLISH = "en"

    fun normalize(preference: String?): String = when (preference) {
        KOREAN, ENGLISH -> preference
        else -> AUTO
    }

    fun resolve(preference: String?, systemLanguage: String?): String {
        val normalized = normalize(preference)
        return if (normalized != AUTO) normalized
            else if (systemLanguage.equals(KOREAN, ignoreCase = true)) KOREAN
            else ENGLISH
    }
}

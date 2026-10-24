package com.elyedali.englishsteps.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

data class AppLanguage(val code: String, val nativeName: String, val flag: String, val rtl: Boolean)

/** لغات واجهة التطبيق: العربية والفرنسية والإنجليزية */
object I18n {
    val languages = listOf(
        AppLanguage("ar", "العربية", "🇲🇷", rtl = true),
        AppLanguage("fr", "Français", "🇫🇷", rtl = false),
        AppLanguage("en", "English", "🇬🇧", rtl = false),
    )

    var lang by mutableStateOf("ar")
        private set

    val isRtl: Boolean get() = languages.firstOrNull { it.code == lang }?.rtl ?: true

    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** هل اختار المستخدم لغته من قبل؟ */
    fun hasChosen(context: Context): Boolean = prefs(context).contains("lang")

    fun init(context: Context) {
        val saved = prefs(context).getString("lang", null)
        lang = saved ?: Locale.getDefault().language.takeIf { l -> languages.any { it.code == l } } ?: "ar"
    }

    fun set(context: Context, code: String) {
        if (languages.none { it.code == code }) return
        lang = code
        prefs(context).edit().putString("lang", code).apply()
    }
}

/** نص الواجهة باللغات الثلاث: العربية، الفرنسية، الإنجليزية */
fun t(ar: String, fr: String, en: String): String = when (I18n.lang) {
    "fr" -> fr
    "en" -> en
    else -> ar
}

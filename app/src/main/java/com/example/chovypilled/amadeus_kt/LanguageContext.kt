package com.example.chovypilled.amadeus_kt

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

class LanguageContext(base: Context?) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context): ContextWrapper {
            var config: Configuration = context.resources.configuration
            var settings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            var language = settings.getString("lang", "en")
            var languageArray: List<String> = language!!.split("-")

            lateinit var locale: Locale
            if (languageArray.size == 3) {
                locale = Locale(languageArray[0], languageArray[1], languageArray[2])
            } else if (languageArray.size == 2) {
                locale = Locale(languageArray[0], languageArray[1])
            } else {
                locale = Locale(languageArray[0])
            }

            Locale.setDefault(locale)
            config.setLocale(locale)
            context.createConfigurationContext(config)
            return LanguageContext(context)
        }

        fun load (context: Context, language: String): Context {
            var config: Configuration = context.resources.configuration
            var locale: Locale = Locale(language)
            Locale.setDefault(locale)

            config.setLocale(locale)
            return context
        }
    }
}
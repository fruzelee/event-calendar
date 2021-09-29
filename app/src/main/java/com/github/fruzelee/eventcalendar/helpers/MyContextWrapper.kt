package com.github.fruzelee.eventcalendar.helpers

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.*

/**
 * @author fazle
 * Created 9/25/2021 at 12:58 PM
 * github.com/fruzelee
 * web: fr.crevado.com
 */

// language forcing used at "Use english language", taken from https://stackoverflow.com/a/40704077/1967672
class MyContextWrapper(context: Context) : ContextWrapper(context) {

    fun wrap(context: Context, language: String): ContextWrapper {
        var newContext = context
        val config = newContext.resources.configuration

        val sysLocale: Locale? = if (isNougatPlus()) {
            getSystemLocale(config)
        } else {
            getSystemLocaleLegacy(config)
        }

        if (language != "" && sysLocale!!.language != language) {
            val locale = Locale(language)
            Locale.setDefault(locale)
            if (isNougatPlus()) {
                setSystemLocale(config, locale)
            } else {
                setSystemLocaleLegacy(config, locale)
            }
        }

        newContext = newContext.createConfigurationContext(config)
        return MyContextWrapper(newContext)
    }

    @Suppress("DEPRECATION")
    private fun getSystemLocaleLegacy(config: Configuration) = config.locale

    @TargetApi(Build.VERSION_CODES.N)
    private fun getSystemLocale(config: Configuration) = config.locales.get(0)

    @Suppress("DEPRECATION")
    private fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
        config.locale = locale
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setSystemLocale(config: Configuration, locale: Locale) {
        config.setLocale(locale)
    }
}

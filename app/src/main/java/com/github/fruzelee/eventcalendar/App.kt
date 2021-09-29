package com.github.fruzelee.eventcalendar

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.github.fruzelee.eventcalendar.extensions.baseConfig
import com.github.fruzelee.eventcalendar.helpers.isNougatPlus
import java.util.*

@Suppress("warnings")
class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }

    @Suppress("DEPRECATION")
    private fun Application.checkUseEnglish() {
        if (baseConfig.useEnglish && !isNougatPlus()) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
    }

}
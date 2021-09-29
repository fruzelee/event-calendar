package com.github.fruzelee.eventcalendar.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.adapters.SelectTimeZoneAdapter
import com.github.fruzelee.eventcalendar.helpers.CURRENT_TIME_ZONE
import com.github.fruzelee.eventcalendar.helpers.TIME_ZONE
import com.github.fruzelee.eventcalendar.helpers.getAllTimeZones
import com.github.fruzelee.eventcalendar.models.MyTimeZone
import kotlinx.android.synthetic.main.activity_select_time_zone.*
import java.util.*

class SelectTimeZoneActivity : BaseActivity() {
    private val allTimeZones = getAllTimeZones()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_time_zone)
        title = ""

        SelectTimeZoneAdapter(this, allTimeZones) {
            val data = Intent()
            data.putExtra(TIME_ZONE, it as MyTimeZone)
            setResult(RESULT_OK, data)
            finish()
        }.apply {
            select_time_zone_list.adapter = this
        }

        val currentTimeZone = intent.getStringExtra(CURRENT_TIME_ZONE) ?: TimeZone.getDefault().id
        val pos = allTimeZones.indexOfFirst { it.zoneName.equals(currentTimeZone, true) }
        if (pos != -1) {
            select_time_zone_list.scrollToPosition(pos)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return true
    }

}

package com.github.fruzelee.eventcalendar.models

data class EveCalCalendar(val id: Int, val displayName: String, val accountName: String, val accountType: String, val ownerName: String,
                          var color: Int, val accessLevel: Int) {
    fun canWrite() = accessLevel >= 500

}

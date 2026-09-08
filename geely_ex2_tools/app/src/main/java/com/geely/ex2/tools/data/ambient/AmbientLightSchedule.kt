package com.geely.ex2.tools.data.ambient

import java.util.Calendar
import java.util.Locale

object AmbientLightSchedule {
    fun shouldBeEnabled(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        hour: Int,
        minute: Int,
    ): Boolean {
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        val now = hour * 60 + minute

        if (start == end) {
            return false
        }

        return if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }

    fun shouldBeEnabledNow(
        context: android.content.Context,
        calendar: Calendar = Calendar.getInstance(),
    ): Boolean {
        return shouldBeEnabled(
            startHour = AmbientLightSettings.getStartHour(context),
            startMinute = AmbientLightSettings.getStartMinute(context),
            endHour = AmbientLightSettings.getEndHour(context),
            endMinute = AmbientLightSettings.getEndMinute(context),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
        )
    }

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)
}

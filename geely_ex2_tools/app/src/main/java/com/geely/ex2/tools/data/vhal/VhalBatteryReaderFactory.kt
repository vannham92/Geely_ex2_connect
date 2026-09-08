package com.geely.ex2.tools.data.vhal

import android.content.Context

object VhalBatteryReaderFactory {
    fun create(context: Context): VhalBatteryReader = CarPropertyBatteryVhalReader(context)
}

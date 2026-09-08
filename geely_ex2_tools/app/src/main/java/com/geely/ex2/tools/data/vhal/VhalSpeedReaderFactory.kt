package com.geely.ex2.tools.data.vhal

import android.content.Context

object VhalSpeedReaderFactory {
    fun create(context: Context): VhalSpeedReader = CarPropertyVhalReader(context)
}

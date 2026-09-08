package com.geely.ex2.tools.data.ambient

import android.content.Context
import com.geely.ex2.tools.data.vhal.AmbientLightSample
import com.geely.ex2.tools.data.vhal.AmbientLightWriteResult
import com.geely.ex2.tools.data.vhal.CarPropertyAmbientLightReader
import com.geely.ex2.tools.data.vhal.CarPropertyIo

class AmbientLightRepository(context: Context) {
    private val appContext = context.applicationContext

    fun readAmbientLight(): AmbientLightSample = CarPropertyIo.call {
        sharedReader(appContext).readAmbientLight()
    }

    fun setAmbientLightEnabled(enabled: Boolean): AmbientLightWriteResult = CarPropertyIo.call {
        sharedReader(appContext).writeAmbientLight(enabled)
    }

    companion object {
        @Volatile
        private var reader: CarPropertyAmbientLightReader? = null

        private fun sharedReader(context: Context): CarPropertyAmbientLightReader {
            return reader ?: synchronized(this) {
                reader ?: CarPropertyAmbientLightReader(context.applicationContext).also { reader = it }
            }
        }
    }
}

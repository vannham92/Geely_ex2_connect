package com.geely.ex2.tools.data.driving

import android.content.Context
import com.geely.ex2.tools.data.vhal.CarPropertyDrivingModeReader
import com.geely.ex2.tools.data.vhal.CarPropertyEnergyRegenerationReader
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.DrivingModeSample
import com.geely.ex2.tools.data.vhal.DrivingModeWriteResult
import com.geely.ex2.tools.data.vhal.EnergyRegenerationSample
import com.geely.ex2.tools.data.vhal.EnergyRegenerationWriteResult

class DrivingModeRepository(private val context: Context) {
    private val appContext = context.applicationContext

    fun isPersistEnabled(): Boolean = DrivingSettings.isPersistEnabled(appContext)

    fun setPersistEnabled(enabled: Boolean) {
        DrivingSettings.setPersistEnabled(appContext, enabled)
    }

    fun getSavedModeValue(): Int = DrivingSettings.getSavedModeValue(appContext)

    fun saveSelectedMode(modeValue: Int) {
        DrivingSettings.setSavedModeValue(appContext, modeValue)
    }

    fun isRegenPersistEnabled(): Boolean = DrivingSettings.isRegenPersistEnabled(appContext)

    fun setRegenPersistEnabled(enabled: Boolean) {
        DrivingSettings.setRegenPersistEnabled(appContext, enabled)
    }

    fun getSavedRegenValue(): Int = DrivingSettings.getSavedRegenValue(appContext)

    fun saveSelectedRegen(levelValue: Int) {
        DrivingSettings.setSavedRegenValue(appContext, levelValue)
    }

    fun startRestoreService(reason: String) {
        DrivingAppStarter.startRestoreServiceIfEnabled(appContext, reason)
    }

    fun stopRestoreService(reason: String) {
        DrivingAppStarter.stopRestoreServiceIfIdle(appContext, reason)
    }

    fun restoreSavedModeIfNeeded(reason: String) {
        DrivingModeController.restoreDrivingModeIfNeeded(appContext, reason)
    }

    fun restoreSavedRegenIfNeeded(reason: String) {
        EnergyRegenController.restoreEnergyRegenerationIfNeeded(appContext, reason)
    }

    fun readDrivingMode(): DrivingModeSample = CarPropertyIo.call {
        sharedModeReader(appContext).readDrivingMode()
    }

    fun setDrivingMode(modeValue: Int): DrivingModeWriteResult = CarPropertyIo.call {
        sharedModeReader(appContext).writeDrivingMode(modeValue)
    }

    fun readEnergyRegeneration(): EnergyRegenerationSample = CarPropertyIo.call {
        sharedRegenReader(appContext).readEnergyRegeneration()
    }

    fun setEnergyRegeneration(levelValue: Int): EnergyRegenerationWriteResult = CarPropertyIo.call {
        sharedRegenReader(appContext).writeEnergyRegeneration(levelValue)
    }

    companion object {
        @Volatile
        private var modeReader: CarPropertyDrivingModeReader? = null

        @Volatile
        private var regenReader: CarPropertyEnergyRegenerationReader? = null

        private fun sharedModeReader(context: Context): CarPropertyDrivingModeReader {
            return modeReader ?: synchronized(this) {
                modeReader ?: CarPropertyDrivingModeReader(context.applicationContext).also {
                    modeReader = it
                }
            }
        }

        private fun sharedRegenReader(context: Context): CarPropertyEnergyRegenerationReader {
            return regenReader ?: synchronized(this) {
                regenReader ?: CarPropertyEnergyRegenerationReader(context.applicationContext).also {
                    regenReader = it
                }
            }
        }
    }
}

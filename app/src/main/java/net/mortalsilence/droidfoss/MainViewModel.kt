package net.mortalsilence.droidfoss

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mortalsilence.droidfoss.comm.DanfossAirUnit
import net.mortalsilence.droidfoss.comm.DanfossAirUnitCommunicationController
import net.mortalsilence.droidfoss.comm.Mode
import net.mortalsilence.droidfoss.discovery.DanfossAirUnitDiscoveryService
import net.mortalsilence.droidfoss.discovery.DiscoveryCache.DISCOVERY_CACHE_INSTANCE
import java.net.InetAddress.getByName
import java.time.ZonedDateTime

data class MainState(
    val snackbarMessage: String? = null,
    val mode: Mode,
    val boost: Boolean? = null,
    val supplyFanSpeed: Short? = null,
    val extractFanSpeed: Short? = null,
    val unitName: String? = null,
    val unitSerialNo: String? = null,
    val manualFanStep: Int? = null,
    val filterLife: Float? = null,
    val filterPeriod: Byte? = null,
    val supplyFanStep: Byte? = null,
    val extractFanStep: Byte? = null,
    val nightCooling: Boolean? = null,
    val bypass: Boolean? = null,
    val roomTemp: Float? = null,
    val roomTempCalculated: Float? = null,
    val outdoorTemp: Float? = null,
    val supplyTemp: Float? = null,
    val extractTemp: Float? = null,
    val exhaustTemp: Float? = null,
    val batteryLife: Int? = null,
    val currentTime: ZonedDateTime? = null,

)

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    var mainState by mutableStateOf(MainState(mode = Mode.NA))
        private set
    private var fetchJob: Job? = null

    fun sendMessage(message: String) {
        mainState = mainState.copy(snackbarMessage = message)
    }

    fun markMessageShown() {
        mainState = mainState.copy(snackbarMessage = null)
    }

    fun fetchData() {
        performWithAirUnit { airUnit ->
            mainState = mainState.copy(
                mode = airUnit.mode,
                boost = airUnit.boost,
                supplyFanSpeed = airUnit.supplyFanSpeed,
                extractFanSpeed = airUnit.extractFanSpeed,
                unitName = airUnit.unitName,
                unitSerialNo = airUnit.unitSerialNumber,
                manualFanStep = airUnit.manualFanStep,
                filterLife = airUnit.filterLife,
                filterPeriod = airUnit.filterPeriod,
                supplyFanStep = airUnit.supplyFanStep,
                extractFanStep = airUnit.extractFanStep,
                nightCooling = airUnit.nightCooling,
                bypass = airUnit.bypass,
                roomTemp = airUnit.roomTemperature,
                roomTempCalculated = airUnit.roomTemperatureCalculated,
                outdoorTemp = airUnit.outdoorTemperature,
                supplyTemp = airUnit.supplyTemperature,
                extractTemp = airUnit.extractTemperature,
                exhaustTemp = airUnit.exhaustTemperature,
                batteryLife = airUnit.batteryLife,
                currentTime = airUnit.currentTime

            )
        }
    }

    fun setMode(mode: Mode) {
        performWithAirUnit { airUnit ->
            airUnit.mode = mode
            mainState = mainState.copy(mode = airUnit.mode)
        }
    }

    fun setBoost(boost: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.boost = boost
            mainState = mainState.copy(boost = airUnit.boost)
        }
    }

    fun setBypass(bypass: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.bypass = bypass
            mainState = mainState.copy(bypass = airUnit.bypass)
        }
    }

    fun setNightCooling(nightCooling: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.nightCooling = nightCooling
            mainState = mainState.copy(nightCooling = airUnit.nightCooling)
        }
    }

    fun setManualFanStep(step: Int) {
        Log.d(TAG, "setManualFanStep $step")
        performWithAirUnit { airUnit ->
            airUnit.manualFanStep = step
            mainState = mainState.copy(manualFanStep = airUnit.manualFanStep)
        }
    }

    fun performWithAirUnit(action: (airUnit: DanfossAirUnit) -> Unit) {
        sendMessage("Sending AirUnit request...")
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(context = Dispatchers.IO) {
            try {
                if (BuildConfig.AIR_UNIT_IP.isNotBlank()) {
                    DISCOVERY_CACHE_INSTANCE.host = BuildConfig.AIR_UNIT_IP
                } else {
                    DanfossAirUnitDiscoveryService().scanForDevice()
                }
                if (DISCOVERY_CACHE_INSTANCE.host == null) {
                    Log.i(TAG, "No AirUnit found...")
                    sendMessage("No Danfoss AirUnit found in network")
                    return@launch
                }
                val commController =
                    DanfossAirUnitCommunicationController(getByName(DISCOVERY_CACHE_INSTANCE.host))
                val airUnit = DanfossAirUnit(commController)
                action.invoke(airUnit)
                sendMessage("Air unit request successful!")
            } catch (e: Exception) {
                e.printStackTrace()
                sendMessage("AirUnit request failed: ${e.message}")
            }
        }
    }
}
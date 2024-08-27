package net.mortalsilence.droidfoss

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
import java.math.BigDecimal
import java.net.InetAddress.getByName

data class MainState(
    val snackbarMessage: String? = null,
    val mode: Mode,
    val boost: Boolean? = null,
    val supplyFanSpeed: Short? = null,
    val extractFanSpeed: Short? = null
)

class MainViewModel : ViewModel() {

    var mainState by mutableStateOf(MainState(mode = Mode.UNKNOWN))
        private set
    private var fetchJob: Job? = null;

    fun sendMessage(message: String) {
        mainState = mainState.copy(snackbarMessage = message)
    }

    fun markMessageShown(message: String) {
        mainState = mainState.copy(snackbarMessage = null)
    }

    fun fetchData() {
        performWithAirUnit { airUnit -> mainState = mainState.copy(
            mode = airUnit.mode,
            boost = airUnit.boost,
            supplyFanSpeed = airUnit.supplyFanSpeed,
            extractFanSpeed = airUnit.extractFanSpeed
        ) }
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

    fun performWithAirUnit(action: (airUnit: DanfossAirUnit) -> Unit) {
        sendMessage("Sending AirUnit request...")
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch(context = Dispatchers.IO) {
            try {
                DanfossAirUnitDiscoveryService().scanForDevice()
                val commController =
                    DanfossAirUnitCommunicationController(getByName(DISCOVERY_CACHE_INSTANCE.host))
                val airUnit = DanfossAirUnit(commController)
                action.invoke(airUnit)
                sendMessage("Air unit request successful!")
            } catch (e: Exception) {
                e.printStackTrace()
                sendMessage("AirUnit request failed: " + e.message)
            }

        }
    }
}
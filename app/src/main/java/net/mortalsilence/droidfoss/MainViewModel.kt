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
import java.net.InetAddress.getByName

data class MainState(
    val snackbarMessages: MutableList<String> = mutableListOf(),
    val mode: Mode
)

class MainViewModel : ViewModel() {

    var mainState by mutableStateOf(MainState(mode = Mode.UNKNOWN))
        private set
    private var fetchJob: Job? = null;

    fun sendMessage(message: String) {
        mainState = mainState.copy(snackbarMessages = mainState.snackbarMessages.toMutableList().apply { add(message) })
    }

    fun markMessageShown(message: String) {
        mainState = mainState.copy(snackbarMessages = mainState.snackbarMessages.toMutableList().apply { remove(message) })
    }

    fun fetchMode() {
        performWithAirUnit { airUnit -> mainState = mainState.copy(mode = airUnit.mode) }
    }

    fun setMode(mode: Mode) {
        performWithAirUnit { airUnit ->
            airUnit.mode = mode
            mainState = mainState.copy(mode = airUnit.mode)
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
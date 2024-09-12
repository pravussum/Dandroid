package net.mortalsilence.dandroid

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mortalsilence.dandroid.backgroundsync.AirUnitNotAvailable
import net.mortalsilence.dandroid.comm.AirUnitAccessor
import net.mortalsilence.dandroid.comm.DanfossAirUnit
import net.mortalsilence.dandroid.comm.Mode
import net.mortalsilence.dandroid.data.AirUnitState
import net.mortalsilence.dandroid.repository.AirUnitStateRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val airUnitStateRepository: AirUnitStateRepository,
    val airUnitAccessor: AirUnitAccessor
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    var airUnitState by mutableStateOf(AirUnitState(mode = Mode.NA))
        private set

    var snackbarMessage by (mutableStateOf(null as String?))
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    init {
        observeRepository()
        fetchData()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            airUnitStateRepository.airUnitState.collect { stateUpdate ->
                Log.d(TAG, "Received state update from repository...")
                airUnitState = stateUpdate
                isRefreshing = false
            }
        }
    }


    private var fetchJob: Job? = null

    fun sendMessage(message: String) {
        snackbarMessage = message
    }

    fun markMessageShown() {
        snackbarMessage = null
    }

    fun setMode(mode: Mode) {
        performWithAirUnit { airUnit ->
            airUnit.mode = mode
            airUnitState = airUnitState.copy(mode = airUnit.mode)
        }
    }

    fun setBoost(boost: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.boost = boost
            airUnitState = airUnitState.copy(boost = airUnit.boost)
        }
    }

    fun setBypass(bypass: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.bypass = bypass
            airUnitState = airUnitState.copy(bypass = airUnit.bypass)
        }
    }

    fun setNightCooling(nightCooling: Boolean) {
        performWithAirUnit { airUnit ->
            airUnit.nightCooling = nightCooling
            airUnitState = airUnitState.copy(nightCooling = airUnit.nightCooling)
        }
    }

    fun setManualFanStep(step: Int) {
        Log.d(TAG, "setManualFanStep $step")
        performWithAirUnit { airUnit ->
            airUnit.manualFanStep = step
            airUnitState = airUnitState.copy(manualFanStep = airUnit.manualFanStep)
        }
    }

    fun fetchData() {
        isRefreshing = true
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                airUnitAccessor.fetchData()
            } catch (e: AirUnitNotAvailable) {
                isRefreshing = false
                sendMessage("Air unit not available!")
            }
        }
    }

    private fun performWithAirUnit(action: (airUnit: DanfossAirUnit) -> Unit) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                airUnitAccessor.performWithAirUnit(action)
            } catch (e: AirUnitNotAvailable) {
                isRefreshing = false
                sendMessage("Air unit not available")
            }
        }
    }
}
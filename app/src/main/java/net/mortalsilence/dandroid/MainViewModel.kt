package net.mortalsilence.dandroid

import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.mortalsilence.dandroid.backgroundsync.AirUnitNotFound
import net.mortalsilence.dandroid.backgroundsync.AirUnitRequestFailed
import net.mortalsilence.dandroid.backgroundsync.AirUnitRequestTimeout
import net.mortalsilence.dandroid.comm.AirUnitAccessor
import net.mortalsilence.dandroid.comm.DanfossAirUnit
import net.mortalsilence.dandroid.comm.Mode
import net.mortalsilence.dandroid.comm.discovery.DiscoveryCache.DISCOVERY_CACHE_INSTANCE
import net.mortalsilence.dandroid.data.AirUnitState
import net.mortalsilence.dandroid.data.AppSettings
import net.mortalsilence.dandroid.repository.AirUnitStateRepository
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")
val IP_ADDRESS = stringPreferencesKey("ip_address")

@HiltViewModel
class MainViewModel @Inject constructor(
    val airUnitStateRepository: AirUnitStateRepository,
    val airUnitAccessor: AirUnitAccessor,
    val application: Application
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

    var preferences by mutableStateOf(AppSettings("", false))

    init {
        observeRepository()
        fetchData()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            application.applicationContext.dataStore.data
                .map { dataStorePrefs ->
                    val dataStoreIpAddress = dataStorePrefs[IP_ADDRESS] ?: ""
                    Log.d(TAG, "Read ip address $dataStoreIpAddress from dataStore.")
                    preferences = preferences.copy(ipAddress = dataStoreIpAddress)
                    DISCOVERY_CACHE_INSTANCE.host = dataStoreIpAddress
                }.first()
        }
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
            } catch (e: AirUnitNotFound) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_no_air_unit_found))
            } catch (e: AirUnitRequestTimeout) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_cannot_reach_air_unit))
            } catch (e: AirUnitRequestFailed) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_air_unit_request_failed, e.message))
            }
        }
    }

    private fun performWithAirUnit(action: (airUnit: DanfossAirUnit) -> Unit) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                airUnitAccessor.performWithAirUnit(action)
            } catch (e: AirUnitNotFound) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_no_air_unit_found))
            } catch (e: AirUnitRequestTimeout) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_cannot_reach_air_unit))
            } catch (e: AirUnitRequestFailed) {
                isRefreshing = false
                sendMessage(application.applicationContext.getString(R.string.error_air_unit_request_failed, e.message))
            }
        }
    }

    fun setIpAddress(ipAddress: String) {
        preferences = preferences.copy(ipAddress = ipAddress)
        if(Patterns.IP_ADDRESS.matcher(ipAddress).matches() || ipAddress.isBlank()) {
            preferences = preferences.copy(ipValid = true)
            storeIpAddress(ipAddress)
        } else {
            preferences = preferences.copy(ipValid = false)
        }
    }

    fun storeIpAddress(ipAddress: String) {
        viewModelScope.launch {
            application.applicationContext.dataStore.edit { dataStorePrefs ->
                Log.d(TAG, "Writing ip address $ipAddress to dataStore.")
                dataStorePrefs[IP_ADDRESS] = ipAddress
                DISCOVERY_CACHE_INSTANCE.host = ipAddress
            }
        }
    }
}
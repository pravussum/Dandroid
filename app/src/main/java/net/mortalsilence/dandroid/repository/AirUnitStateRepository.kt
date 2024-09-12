package net.mortalsilence.dandroid.repository

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.mortalsilence.dandroid.comm.Mode
import net.mortalsilence.dandroid.data.AirUnitState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirUnitStateRepository @Inject constructor() {

    private val airUnitStateFlow = MutableStateFlow<AirUnitState>(AirUnitState(mode = Mode.NA))

    companion object {
        private const val TAG = "AirUnitStateRepository"
    }

    val airUnitState = airUnitStateFlow.asStateFlow()

    fun save(airUnitState: AirUnitState) {
        Log.i(TAG, "Saving in repository: ${airUnitState.unitName}")
        airUnitStateFlow.value = airUnitState
    }
}
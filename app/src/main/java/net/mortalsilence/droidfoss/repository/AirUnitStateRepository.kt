package net.mortalsilence.droidfoss.repository

import android.util.Log
import net.mortalsilence.droidfoss.data.AirUnitState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirUnitStateRepository @Inject constructor(){

    companion object {
        private const val TAG = "AirUnitStateRepository"
    }

    fun save(airUnitState: AirUnitState) {
        Log.i(TAG, "Saving in repository: ${airUnitState.unitName}")
    }
}
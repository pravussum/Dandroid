package net.mortalsilence.droidfoss.comm

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import net.mortalsilence.droidfoss.BuildConfig
import net.mortalsilence.droidfoss.backgroundsync.AirUnitRequestFailed
import net.mortalsilence.droidfoss.comm.discovery.DanfossAirUnitDiscoveryService
import net.mortalsilence.droidfoss.comm.discovery.DiscoveryCache.DISCOVERY_CACHE_INSTANCE
import net.mortalsilence.droidfoss.data.AirUnitState
import net.mortalsilence.droidfoss.repository.AirUnitStateRepository
import java.net.InetAddress.getByName
import javax.inject.Inject

class AirUnitAccessor @Inject constructor(
    val applicationScope: CoroutineScope,
    val airUnitStateRepository: AirUnitStateRepository){

    companion object {
        private const val TAG = "AirUnitAccessor"
    }

    suspend fun fetchData(): AirUnitState {
        val airUnitState = performWithAirUnit { airUnit ->
            AirUnitState(
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
        } as AirUnitState

        airUnitStateRepository.save(airUnitState)

        return airUnitState
    }

    suspend fun performWithAirUnit(action: (airUnit: DanfossAirUnit) -> Any): Any {
        return applicationScope.async {
            performWithAirUnitInternal(action)
        }.await()
    }

    private fun performWithAirUnitInternal(action: (airUnit: DanfossAirUnit) -> Any): Any {
        try {
            if (BuildConfig.AIR_UNIT_IP.isNotBlank()) {
                DISCOVERY_CACHE_INSTANCE.host = BuildConfig.AIR_UNIT_IP
            } else {
                DanfossAirUnitDiscoveryService()
                    .scanForDevice()
            }
            if (DISCOVERY_CACHE_INSTANCE.host == null) {
                Log.i(TAG, "No AirUnit found...")
                throw AirUnitRequestFailed()
            }
            val commController =
                DanfossAirUnitCommunicationController(getByName(DISCOVERY_CACHE_INSTANCE.host))
            val airUnit = DanfossAirUnit(commController)
            val result = action.invoke(airUnit)
            Log.i(TAG, "Air unit request successful!")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "AirUnit request failed: ${e.message}")
            throw AirUnitRequestFailed(e)
        }
    }
}
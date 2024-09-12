package net.mortalsilence.dandroid.comm

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import net.mortalsilence.dandroid.BuildConfig
import net.mortalsilence.dandroid.backgroundsync.AirUnitNotFound
import net.mortalsilence.dandroid.backgroundsync.AirUnitRequestFailed
import net.mortalsilence.dandroid.backgroundsync.AirUnitRequestTimeout
import net.mortalsilence.dandroid.comm.discovery.DanfossAirUnitDiscoveryService
import net.mortalsilence.dandroid.comm.discovery.DiscoveryCache.DISCOVERY_CACHE_INSTANCE
import net.mortalsilence.dandroid.data.AirUnitState
import net.mortalsilence.dandroid.repository.AirUnitStateRepository
import java.net.InetAddress.getByName
import java.net.SocketTimeoutException
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
       try {
           return applicationScope.async {
               performWithAirUnitInternal(action)
           }.await()
       } catch(e: AirUnitNotFound) {
           throw e
       } catch (e: AirUnitRequestTimeout){
           throw e
       }
    }

    private fun performWithAirUnitInternal(action: (airUnit: DanfossAirUnit) -> Any): Any {

            if (BuildConfig.AIR_UNIT_IP.isNotBlank()) {
                DISCOVERY_CACHE_INSTANCE.host = BuildConfig.AIR_UNIT_IP
            } else {
                DanfossAirUnitDiscoveryService()
                    .scanForDevice()
            }
            if (DISCOVERY_CACHE_INSTANCE.host == null) {
                Log.i(TAG, "No AirUnit found...")
                throw AirUnitNotFound()
            }
        try {
            val commController =
                DanfossAirUnitCommunicationController(getByName(DISCOVERY_CACHE_INSTANCE.host))
            val airUnit = DanfossAirUnit(commController)
            val result = action.invoke(airUnit)
            Log.i(TAG, "Air unit request successful!")
            return result
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "AirUnit request timed out. Maybe air unit already bound?")
            throw AirUnitRequestTimeout(e)
        } catch (e: Exception) {
            Log.e(TAG, "AirUnit request failed: ${e.message}")
            throw AirUnitRequestFailed(e)
        }
    }
}
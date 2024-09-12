package net.mortalsilence.dandroid.data

import net.mortalsilence.dandroid.comm.Mode
import java.time.ZonedDateTime

data class AirUnitState(
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
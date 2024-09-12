package net.mortalsilence.droidfoss.backgroundsync

class AirUnitRequestFailed : Exception {

    constructor() : super()

    constructor(e: Exception) : super(e)
}
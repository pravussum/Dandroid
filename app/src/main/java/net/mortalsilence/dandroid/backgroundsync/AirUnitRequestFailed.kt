package net.mortalsilence.dandroid.backgroundsync

class AirUnitRequestFailed : Exception {

    constructor() : super()

    constructor(e: Exception) : super(e)
}
package com.group7.utils

import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator

/** A collection of useful functions, objects, and classes to be used within Node Tests */
internal data object TestVehicle

internal data object TestVehicle1 : TestVehicleInterface

internal data object TestVehicle2 : TestVehicleInterface

internal data object TestVehicle3 : TestVehicleInterface

interface TestVehicleInterface

internal data object TestContainer

internal data object TestLoadedVehicle

internal const val NUM_VEHICLES = 100

internal const val NUM_CHANNELS = 10

internal enum class VehicleTravelDirection {
    OUTBOUND,
    INBOUND,
}

// Required for fork policies, which need a simulator in context
internal val mockSimulator = Simulator(EventLog.noop(), Scenario(emptyList(), mutableListOf()))

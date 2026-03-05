package com.group7.sweeps

import com.group7.EventLog
import com.group7.Scenario
import com.group7.Simulator
import kotlin.time.Duration

fun <LoggerType : EventLog> runSweeps(scenarios: List<Triple<Scenario, LoggerType, Duration>>): List<Simulator> {
    return scenarios.map { (s, l, d) ->
        val sim = Simulator(l, s)
        sim.runFor(d)
        sim
    }
}

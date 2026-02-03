package com.group7.utils

import com.group7.EventLog
import kotlin.time.Instant

/** A log that keeps track of time of the events, without keeping the message associated with each event */
internal class TimeLog : EventLog {
    private val _timeLog = mutableListOf<Instant>()

    val timeLog: List<Instant>
        get() = _timeLog

    override fun log(time: Instant, message: () -> String) {
        _timeLog += time
    }
}

/** A log that can be queried later on list of timestamps for a specific event occuring on a specific node */
internal class QueryLog : EventLog {
    private val log = mutableListOf<Pair<Instant, String>>()

    override fun log(time: Instant, message: () -> String) {
        log.add(Pair(time, message()))
    }

    fun query(nodeLabel: String, directionality: VehicleTravelDirection): List<Instant> {
        val matchText =
            when (directionality) {
                VehicleTravelDirection.OUTBOUND -> "from "
                VehicleTravelDirection.INBOUND -> "to "
            } + nodeLabel

        return log.asSequence().filter { (_, loggedEvent) -> matchText in loggedEvent }.map { it.first }.toList()
    }
}

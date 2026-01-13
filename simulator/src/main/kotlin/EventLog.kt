package com.group7
import kotlin.time.Instant

interface EventLog {
    fun log(time: Instant, message: String)
}
package com.group7

import kotlin.time.Instant

interface EventLog {
    fun log(time: Instant, message: String)

    companion object {
        fun noop() =
            object : EventLog {
                override fun log(time: Instant, message: String) {}
            }

        fun stdout() =
            object : EventLog {
                override fun log(time: Instant, message: String) {
                    println("[$time] $message")
                }
            }
    }
}

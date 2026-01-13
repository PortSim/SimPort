package com.group7

interface EventLog {
    fun log(time: Instant, message: String)
}
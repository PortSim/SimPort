package com.group7.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import umontreal.ssj.probdist.StudentDist

private val studentCache: ConcurrentMap<StudentKey, Double> = ConcurrentHashMap()

fun studentT(df: Int, alpha: Double): Double =
    studentCache.computeIfAbsent(StudentKey(df, alpha)) { StudentDist.inverseF(df, 1.0 - alpha / 2) }

private data class StudentKey(val df: Int, val alpha: Double)

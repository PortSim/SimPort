package com.group7.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import umontreal.ssj.probdist.StudentDist

/** Caches calculated student-t values previously encountered during execution */
private val studentCache: ConcurrentMap<StudentKey, Double> = ConcurrentHashMap()

/**
 * Calculates the student-T value
 *
 * @param df degrees of freedom
 * @param alpha alpha
 */
fun studentT(df: Int, alpha: Double): Double =
    studentCache.computeIfAbsent(StudentKey(df, alpha)) { StudentDist.inverseF(df, 1.0 - alpha / 2) }

private data class StudentKey(val df: Int, val alpha: Double)

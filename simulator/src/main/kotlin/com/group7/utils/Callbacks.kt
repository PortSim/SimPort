package com.group7.utils

/**
 * Chains `other` lambda function to execute after this lambda function
 *
 * @param other lambda function with the same signature as this function
 */
fun (() -> Unit)?.andThen(other: () -> Unit): () -> Unit =
    if (this == null) {
        other
    } else {
        {
            this()
            other()
        }
    }

/**
 * Chains `other` lambda function to execute after this lambda function with the same inputs
 *
 * @param other lambda function with the same signature as this function
 */
fun <A, B> ((A, B) -> Unit)?.andThen(other: (A, B) -> Unit): (A, B) -> Unit =
    if (this == null) {
        other
    } else {
        { a, b ->
            this(a, b)
            other(a, b)
        }
    }

/**
 * Chains `other` lambda function to execute after this lambda function with the same inputs
 *
 * @param other lambda function with the same signature as this function
 */
fun <A, B, C> ((A, B, C) -> Unit)?.andThen(other: (A, B, C) -> Unit): (A, B, C) -> Unit =
    if (this == null) {
        other
    } else {
        { a, b, c ->
            this(a, b, c)
            other(a, b, c)
        }
    }

/**
 * Chains `other` lambda function to execute after this lambda function with the same inputs
 *
 * @param other lambda function with the same signature as this function
 */
fun <A, B, C, D> ((A, B, C, D) -> Unit)?.andThen(other: (A, B, C, D) -> Unit): (A, B, C, D) -> Unit =
    if (this == null) {
        other
    } else {
        { a, b, c, d ->
            this(a, b, c, d)
            other(a, b, c, d)
        }
    }

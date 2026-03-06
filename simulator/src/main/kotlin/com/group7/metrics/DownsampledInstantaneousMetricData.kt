package com.group7.metrics

import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.collections.immutable.toPersistentList

/**
 * Downsamples to maintain [DESIRED_SAMPLES] samples. This is visually lossy but the scatter plots are only useful
 * to see general patterns.
 * Importantly, every sample has an equal chance of being preserved, via reservoir sampling.
 */
class DownsampledInstantaneousMetricData : MetricData {
    private var totalSamplesSeen = 0L

    // Array for O(1) random eviction (standard Reservoir technique)
    private val reservoir = arrayOfNulls<Sample>(DESIRED_SAMPLES)

    // TreeSet for O(log k) sorted maintenance
    private val sortedSamples = sortedSetOf<Sample>()

    override val values
        get() = sortedSamples.asSequence().map { MetricValue(it.time, it.value) }.toPersistentList()

    /**
     * Data class requires a unique ID tie-breaker so identical timestamps don't accidentally overwrite each other in
     * the TreeSet.
     */
    data class Sample(val sequenceId: Long, val time: Instant, val value: Double) : Comparable<Sample> {
        override fun compareTo(other: Sample): Int {
            val timeCmp = this.time.compareTo(other.time)
            // If times are identical, use sequenceId to keep them distinct in the Set
            return if (timeCmp != 0) timeCmp else this.sequenceId.compareTo(other.sequenceId)
        }
    }

    override fun add(currentTime: Instant, value: Double) {
        val currentId = totalSamplesSeen
        val newSample = Sample(currentId, currentTime, value)
        totalSamplesSeen++

        if (currentId < DESIRED_SAMPLES) {
            // Initial phase: fill both structures
            val index = currentId.toInt()
            reservoir[index] = newSample
            sortedSamples.add(newSample)
        } else {
            // Reservoir is full: Apply Algorithm R
            val r = Random.nextLong(totalSamplesSeen)

            if (r < DESIRED_SAMPLES) {
                val index = r.toInt()

                // Get the victim and remove it from the sorted tree
                val victim = reservoir[index]!!
                sortedSamples.remove(victim)

                // Replace victim in the array
                reservoir[index] = newSample

                // Add the new sample to the sorted tree
                sortedSamples.add(newSample)
            }
        }
    }

    private companion object {
        private const val DESIRED_SAMPLES = 5000
    }
}

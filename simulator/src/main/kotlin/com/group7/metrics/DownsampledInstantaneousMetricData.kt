package com.group7.metrics

import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.collections.immutable.toPersistentList

/**
 * Downsamples instantaneous metric data using reservoir sampling.
 *
 * For instantaneous metrics, this implementation maintains approximately [DESIRED_SAMPLES] samples using Algorithm R
 * (standard reservoir sampling). While this is visually lossy, scatter plots are primarily used to identify general
 * patterns. Every sample has an equal probability of being preserved, providing uniform and unbiased sampling across
 * the entire time series.
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
     * A single metric sample at a specific point in time.
     *
     * The sequence ID serves as a tie-breaker for samples with identical timestamps, ensuring they are treated as
     * distinct in the sorted set and don't accidentally overwrite each other.
     *
     * @property sequenceId unique identifier assigning this sample a position in the overall sequence
     * @property time the simulation time at which this sample was recorded
     * @property value the numeric value of the metric at this time
     */
    data class Sample(val sequenceId: Long, val time: Instant, val value: Double) : Comparable<Sample> {
        override fun compareTo(other: Sample): Int {
            val timeCmp = this.time.compareTo(other.time)
            // If times are identical, use sequenceId to keep them distinct in the Set
            return if (timeCmp != 0) timeCmp else this.sequenceId.compareTo(other.sequenceId)
        }
    }

    /**
     * Adds a new metric value using reservoir sampling (Algorithm R).
     *
     * For the first [DESIRED_SAMPLES] values, all are stored. Afterward, each subsequent value has a [DESIRED_SAMPLES]
     * / [totalSamplesSeen] probability of being included, with older samples having equal probability of being evicted.
     *
     * @param currentTime the simulation time at which the value was recorded
     * @param value the numeric value of the metric
     */
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

    /** Target number of samples to maintain during downsampling. */
    internal companion object {
        internal const val DESIRED_SAMPLES = 5000
    }
}

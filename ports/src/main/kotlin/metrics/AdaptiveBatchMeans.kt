package com.group7.metrics

/**
 * Does batch means on data as new samples come in by:
 * - sorting data into up to `2 * targetBatches` batches, and report the mean of each of these
 * - when `2 * targetBatches` is reached, squash pairs of batches into one batch to get `targetBatches` batches.
 */
internal class AdaptiveBatchMeans(override val targetBatches: Int = 32) : BatchMeans {
    private var batchSize = 1
    private var currentSum = 0.0
    private var currentCount = 0

    private var batchMeans = mutableListOf<Double>()
    private var totalSum = 0.0
    private var totalCount = 0L

    /** Add a new value to the batches. */
    fun add(x: Double) {
        totalSum += x
        totalCount++

        currentSum += x
        currentCount++

        // If the current batch we're working on is full
        // start working on a new batch
        if (currentCount == batchSize) {
            batchMeans.add(currentSum / batchSize)
            currentSum = 0.0
            currentCount = 0

            // If that new batch puts us over the size limit merge batches
            if (batchMeans.size >= 2 * targetBatches) {
                mergeBatches()
            }
        }
    }

    private fun mergeBatches() {
        val merged = mutableListOf<Double>()
        // Squash pairs of batches together
        // The values are means so the mean is preserved simply by averaging the two values
        for (i in batchMeans.indices step 2) {
            merged.add((batchMeans[i] + batchMeans[i + 1]) / 2)
        }
        batchMeans = merged
        batchSize *= 2
    }

    override fun mean(): Double = totalSum / totalCount

    override fun batchCount(): Int = batchMeans.size

    override fun sampleCount(): Long = totalCount

    override fun batchVariance(): Double {
        val b = batchMeans.size
        require(b >= 2)
        val mean = batchMeans.average()
        // Bias-corrected variance is distance from mean squared over samples - 1
        return batchMeans.sumOf { (it - mean) * (it - mean) } / (b - 1)
    }
}

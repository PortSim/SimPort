package com.group7.metrics

internal class AdaptiveBatchMeans(override val targetBatches: Int = 32) : BatchMeans {
    private var batchSize = 1
    private var currentSum = 0.0
    private var currentCount = 0

    private var batchMeans = mutableListOf<Double>()
    private var totalSum = 0.0
    private var totalCount = 0L

    fun add(x: Double) {
        totalSum += x
        totalCount++

        currentSum += x
        currentCount++

        if (currentCount == batchSize) {
            batchMeans.add(currentSum / batchSize)
            currentSum = 0.0
            currentCount = 0

            if (batchMeans.size >= 2 * targetBatches) {
                mergeBatches()
            }
        }
    }

    private fun mergeBatches() {
        val merged = mutableListOf<Double>()
        for (i in batchMeans.indices step 2) {
            merged.add((batchMeans[i] + batchMeans[i + 1]) / 2)
        }
        batchMeans = merged
        batchSize *= 2
    }

    override fun mean(): Double = totalSum / totalCount

    override fun batchCount(): Int = batchMeans.size

    override fun batchVariance(): Double {
        val b = batchMeans.size
        require(b >= 2)
        val mean = batchMeans.average()
        return batchMeans.sumOf { (it - mean) * (it - mean) } / (b - 1)
    }
}

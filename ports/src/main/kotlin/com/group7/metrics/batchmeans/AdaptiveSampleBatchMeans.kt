package com.group7.metrics.batchmeans

internal class AdaptiveSampleBatchMeans(targetBatches: Int = 32) : AdaptiveBatchMeans(targetBatches) {
    private var batchSize = 1
    private var currentSum = 0.0
    private var currentCount = 0

    fun add(x: Double) {
        currentSum += x
        currentCount++

        if (currentCount == batchSize) {
            if (addBatchAndCollapse(currentSum / batchSize)) {
                batchSize *= 2
            }
            currentSum = 0.0
            currentCount = 0
        }
    }
}

package com.group7.metrics.batchmeans

import com.group7.utils.andThen

/**
 * Does batch means on data as new values come in by:
 * - storing data into up to `2 * targetBatches` batches, and report the mean of each of these
 * - when `2 * targetBatches` is reached, squash pairs of batches into one batch to get `targetBatches` batches.
 */
abstract class AdaptiveBatchMeans(val targetBatches: Int) {
    private val batchMeans: MutableList<Double> = ArrayList(targetBatches * 2)
    private var closeBatchCallback: (() -> Unit)? = null

    fun onCloseBatch(callback: () -> Unit) {
        closeBatchCallback = closeBatchCallback.andThen(callback)
    }

    protected fun addBatchAndCollapse(x: Double): Boolean {
        closeBatchCallback?.invoke()
        batchMeans.add(x)
        return collapseBatchesIfNeeded()
    }

    /** Get current number of batches */
    fun batchCount() = batchMeans.size

    /** Get current overall mean */
    fun mean() = batchMeans.average()

    /** Get current variance. Requires at least 2 batches */
    fun batchVariance(): Double {
        val b = batchMeans.size
        require(b >= 2)
        val mean = batchMeans.average()
        // Bias-corrected variance is distance from mean squared over samples - 1
        return batchMeans.sumOf { (it - mean) * (it - mean) } / (b - 1)
    }

    private fun collapseBatchesIfNeeded(): Boolean {
        if (batchMeans.size < 2 * targetBatches) {
            return false
        }
        // Squash pairs of batches together
        // The values are means and the widths are equal so the mean is preserved simply by averaging the two values
        val newCount = batchMeans.size / 2
        for (i in 0..<newCount) {
            val b1 = batchMeans[2 * i]
            val b2 = batchMeans[2 * i + 1]
            batchMeans[i] = (b1 + b2) / 2
        }

        // Remove the tail end of the list
        batchMeans.subList(newCount, batchMeans.size).clear()
        return true
    }
}

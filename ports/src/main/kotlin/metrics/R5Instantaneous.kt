package com.group7.metrics

class R5Instantaneous(metric: InstantaneousMetric, k: Int = 19, period: Int = 500) :
    R5SteadyStateDetector(SampleMean(metric), k, period) {

    init {
        metric.onFire(this::reportSample)
    }
}

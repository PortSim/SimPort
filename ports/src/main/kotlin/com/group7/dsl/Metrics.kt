package com.group7.dsl

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.metrics.GlobalMetricFactory
import com.group7.metrics.MetricFactory
import com.group7.metrics.MetricGroup

context(scenarioScope: ScenarioBuilderScope)
fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.track(
    metricFactory: MetricFactory<NodeT>
): BuilderT {
    scenarioScope.asImpl().metrics.add { scenario ->
        metricFactory.create(this.node, scenario) ?: error("Metric $metricFactory does not support node ${this.node}")
    }
    return this
}

context(metricsScope: MetricsBuilderScope)
inline fun <reified NodeT> trackAll(metricFactory: MetricFactory<NodeT>) {
    trackNotNull { node, scenario -> (node as? NodeT)?.let { metricFactory.create(it, scenario) } }
}

context(metricsScope: MetricsBuilderScope)
fun trackGlobal(metricFactory: GlobalMetricFactory) {
    metricsScope.asImpl().addMetric(metricFactory.create(metricsScope.asImpl().scenario))
}

@PublishedApi
context(metricsScope: MetricsBuilderScope)
internal fun trackNotNull(metricFactory: (NodeGroup, Scenario) -> MetricGroup?) {
    val scenario = metricsScope.asImpl().scenario
    for (node in scenario.allNodeGroups) {
        val metric = metricFactory(node, scenario) ?: continue
        metricsScope.asImpl().addMetric(metric)
    }
}

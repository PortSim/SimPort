package com.group7.dsl

import com.group7.NodeGroup
import com.group7.metrics.GlobalMetricFactory
import com.group7.metrics.MetricFactory
import com.group7.metrics.MetricGroup

context(scenarioScope: ScenarioBuilderScope)
fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.track(
    metricFactory: MetricFactory<NodeT>
): BuilderT {
    scenarioScope
        .asImpl()
        .metrics
        .add(metricFactory.createGroup(this.node) ?: error("Metric $metricFactory does not support node ${this.node}"))
    return this
}

context(metricsScope: MetricsBuilderScope)
inline fun <reified NodeT> trackAll(metricFactory: MetricFactory<NodeT>) {
    trackNotNull({ (it as? NodeT)?.let(metricFactory::createGroup) })
}

context(metricsScope: MetricsBuilderScope)
fun trackGlobal(metricFactory: GlobalMetricFactory) {
    metricsScope.asImpl().metrics.add(metricFactory.createGroup(metricsScope.asImpl().scenario))
}

@PublishedApi
context(metricsScope: MetricsBuilderScope)
internal fun trackNotNull(metricFactory: (NodeGroup) -> MetricGroup?) {
    for (node in metricsScope.asImpl().scenario.allNodes) {
        val metric = metricFactory(node) ?: continue
        metricsScope.asImpl().metrics.add(metric)
    }
}

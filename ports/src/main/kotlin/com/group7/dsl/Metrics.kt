package com.group7.dsl

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.metrics.GlobalMetricFactory
import com.group7.metrics.MetricFactory
import com.group7.metrics.MetricGroup

/**
 * Used inline with the NodeBuilder DSL system.
 *
 * Tracks the immediately upstream node by applying the `metricFactory` to the node.
 *
 * [Read more here.](https://simport.xhirp.com/docs/tutorials/04-metrics.html)
 *
 * @param metricFactory The [MetricFactory] to associate with the node.
 * @throws IllegalStateException if the node preceding this does not support the given metric
 */
context(scenarioScope: ScenarioBuilderScope)
fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.track(
    metricFactory: MetricFactory<NodeT>
): BuilderT {
    scenarioScope.asImpl().metrics.add { scenario ->
        metricFactory.create(this.node, scenario) ?: error("Metric $metricFactory does not support node ${this.node}")
    }
    return this
}

/**
 * Tracks all applicable nodes in the scenario stored in the contextual [MetricsBuilderScope] with the metric factory
 * provided.
 *
 * @param metricFactory The [MetricFactory] to associate with all applicable nodes in the scenario.
 */
context(metricsScope: MetricsBuilderScope)
inline fun <reified NodeT> trackAll(metricFactory: MetricFactory<NodeT>) {
    trackNotNull { node, scenario -> (node as? NodeT)?.let { metricFactory.create(it, scenario) } }
}

/**
 * Tracks global metrics in the scenario stored in the contextual [MetricsBuilderScope] with the metric factory
 * provided.
 *
 * @param metricFactory The [GlobalMetricFactory] to be associated with this scenario
 */
context(metricsScope: MetricsBuilderScope)
fun trackGlobal(metricFactory: GlobalMetricFactory) {
    metricsScope.asImpl().addMetric(metricFactory.create(metricsScope.asImpl().scenario))
}

/**
 * Given `metricFactory`, applies it to all applicable nodes in the scenario stored in the contextual
 * [MetricsBuilderScopeImpl]
 *
 * @param metricFactory Lambda function that will apply some metric collector over node groups in the scenario belonging
 *   to the contextual MetricsBuilder.
 */
@PublishedApi
context(metricsScope: MetricsBuilderScope)
internal fun trackNotNull(metricFactory: (NodeGroup, Scenario) -> MetricGroup?) {
    val scenario = metricsScope.asImpl().scenario
    for (node in scenario.allNodeGroups) {
        val metric = metricFactory(node, scenario) ?: continue
        metricsScope.asImpl().addMetric(metric)
    }
}

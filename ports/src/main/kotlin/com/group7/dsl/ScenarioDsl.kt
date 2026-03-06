package com.group7.dsl

import com.group7.GroupScope
import com.group7.Scenario
import com.group7.SourceNode
import com.group7.metrics.MetricGroup
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Keeps track of the scenario being built by the DSL system, and abstracts explicit node and connection handling
 * required to construct the port otherwise
 */
sealed interface ScenarioBuilderScope

/**
 * Keeps track of metrics requested by the metrics DSL system, to be installed into a given scenario, and abstracts away
 * explicit metric creation, tag walking, and metrics object handling
 */
sealed interface MetricsBuilderScope

/**
 * Builds a scenario using the DSL system.
 *
 * [Read more](https://simport.xhirp.com/docs/tutorials/02-comparing-simulations.html) to see how `buildScenario` is
 * used to define a port.
 *
 * @param builder Lambda function containing the DSL chain
 */
fun buildScenario(
    builder:
        context(ScenarioBuilderScope)
        () -> Unit
): Scenario {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val scenarioScope = ScenarioBuilderScopeImpl()
    GroupScope.withGroup(null) { context(scenarioScope) { builder() } }
    val scenario = Scenario(scenarioScope.asImpl().sources)
    for (metric in scenarioScope.asImpl().metrics) {
        scenario.addMetric(metric(scenario))
    }
    return scenario
}

/**
 * Attaches the metrics DSL calls within `builder` to this scenario
 *
 * @param builder Lambda function containing metric DSL items
 */
fun Scenario.withMetrics(builder: MetricsBuilderScope.() -> Unit): Scenario {
    MetricsBuilderScopeImpl(this).apply(builder)
    return this
}

/** Internal implementation of [ScenarioBuilderScope] */
internal class ScenarioBuilderScopeImpl : ScenarioBuilderScope {
    val sources = mutableListOf<SourceNode>()
    val metrics = mutableListOf<(Scenario) -> MetricGroup>()
}

/** Returns the internal implementation of [ScenarioBuilderScope] */
internal fun ScenarioBuilderScope.asImpl() =
    when (this) {
        is ScenarioBuilderScopeImpl -> this
    }

/** Internal implementation of [MetricsBuilderScope] */
internal class MetricsBuilderScopeImpl(val scenario: Scenario) : MetricsBuilderScope {
    fun addMetric(metric: MetricGroup) {
        scenario.addMetric(metric)
    }
}

/** Returns the internal implementation of [MetricsBuilderScope] */
internal fun MetricsBuilderScope.asImpl() =
    when (this) {
        is MetricsBuilderScopeImpl -> this
    }

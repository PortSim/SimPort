package com.group7.dsl

import com.group7.GroupScope
import com.group7.Scenario
import com.group7.SourceNode
import com.group7.metrics.MetricGroup
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface ScenarioBuilderScope

sealed interface MetricsBuilderScope

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

fun Scenario.withMetrics(builder: MetricsBuilderScope.() -> Unit): Scenario {
    MetricsBuilderScopeImpl(this).apply(builder)
    return this
}

internal class ScenarioBuilderScopeImpl : ScenarioBuilderScope {
    val sources = mutableListOf<SourceNode>()
    val metrics = mutableListOf<(Scenario) -> MetricGroup>()
}

internal fun ScenarioBuilderScope.asImpl() =
    when (this) {
        is ScenarioBuilderScopeImpl -> this
    }

internal class MetricsBuilderScopeImpl(val scenario: Scenario) : MetricsBuilderScope {
    fun addMetric(metric: MetricGroup) {
        scenario.addMetric(metric)
    }
}

internal fun MetricsBuilderScope.asImpl() =
    when (this) {
        is MetricsBuilderScopeImpl -> this
    }

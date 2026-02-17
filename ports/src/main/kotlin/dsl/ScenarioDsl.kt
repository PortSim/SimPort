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
    return Scenario(scenarioScope.asImpl().sources, scenarioScope.asImpl().metrics)
}

fun Scenario.withMetrics(builder: MetricsBuilderScope.() -> Unit): Scenario {
    MetricsBuilderScopeImpl(this).apply(builder)
    return this
}

internal class ScenarioBuilderScopeImpl : ScenarioBuilderScope {
    val sources = mutableListOf<SourceNode>()
    val metrics = mutableSetOf<MetricGroup>()
}

internal fun ScenarioBuilderScope.asImpl() =
    when (this) {
        is ScenarioBuilderScopeImpl -> this
    }

internal class MetricsBuilderScopeImpl(val scenario: Scenario) : MetricsBuilderScope {
    val metrics = scenario.metrics
}

internal fun MetricsBuilderScope.asImpl() =
    when (this) {
        is MetricsBuilderScopeImpl -> this
    }

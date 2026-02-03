package com.group7.dsl

import com.group7.Scenario
import com.group7.SourceNode
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface ScenarioBuilderScope

inline fun buildScenario(
    builder:
        context(ScenarioBuilderScope, GroupScope)
        () -> Unit
): Scenario {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val scenarioScope = ScenarioBuilderScope()
    context(scenarioScope, GroupScope.Root) { builder() }
    return Scenario(scenarioScope.sources)
}

private class ScenarioBuilderScopeImpl : ScenarioBuilderScope {
    val sources = mutableListOf<SourceNode>()
}

private fun ScenarioBuilderScope.asImpl() =
    when (this) {
        is ScenarioBuilderScopeImpl -> this
    }

@PublishedApi internal fun ScenarioBuilderScope(): ScenarioBuilderScope = ScenarioBuilderScopeImpl()

@PublishedApi
internal val ScenarioBuilderScope.sources
    get() = this.asImpl().sources

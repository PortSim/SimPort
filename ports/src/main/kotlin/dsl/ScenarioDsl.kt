package com.group7.dsl

import com.group7.Scenario
import com.group7.SourceNode
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface ScenarioBuilderScope

inline fun buildScenario(
    builder:
        context(ScenarioBuilderScope)
        () -> Unit
): Scenario {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val scope = ScenarioBuilderScope()
    context(scope) { builder() }
    return Scenario(scope.sources)
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

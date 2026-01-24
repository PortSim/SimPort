package com.group7.utils

import com.group7.dsl.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private data object Token

context(_: ScenarioBuilderScope)
fun <InputT, OutputT> NodeBuilder<InputT>.thenSubnetwork(
    networkName: String? = null,
    capacity: Int,
    inner: (NodeBuilder<InputT>) -> NodeBuilder<OutputT>,
): NodeBuilder<OutputT> {
    contract { callsInPlace(inner, InvocationKind.EXACTLY_ONCE) }

    val namePrefix = networkName?.let { "$it " } ?: ""

    val tokenBackEdge = newConnection<Token>()
    // TODO: This should use a dedicated queue policy to model the tokens as an integer rather than a List
    val tokenQueue = tokenBackEdge.thenQueue("${namePrefix}Token Queue", List(capacity) { Token })

    match("${namePrefix}Token Match", this, tokenQueue) { input, _ -> input }
        .let(inner)
        .thenSplit("${namePrefix}Token Split") { output -> output to Token }
        .let { (outputs, tokens) ->
            tokens.thenConnect(tokenBackEdge)

            return outputs
        }
}

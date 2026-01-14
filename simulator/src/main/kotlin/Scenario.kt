package com.group7

class Scenario(val sources: List<SourceNode>) {
    constructor(vararg sources: SourceNode) : this(sources.toList())
}

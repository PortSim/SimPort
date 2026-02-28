package utils

import org.eclipse.elk.graph.ElkNode

val ElkNode.isRoot
    get() = parent == null

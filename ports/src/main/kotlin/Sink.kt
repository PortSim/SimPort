package com.group7

import com.group7.InputChannel
import com.group7.Node

class Sink<EventT, InputT>(
    label: String,
    incoming: List<InputChannel<InputT>>,
) : Node<EventT, InputT, Nothing>(label, incoming, emptyList()) {

}
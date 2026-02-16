package com.group7.policies.generic_fj

class FirstAvailablePolicy<ChannelT> : PriorityPolicy<ChannelT>(Int::compareTo)

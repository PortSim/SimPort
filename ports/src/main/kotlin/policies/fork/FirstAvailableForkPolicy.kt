package com.group7.policies.fork

class FirstAvailableForkPolicy<T> : PriorityForkPolicy<T>(Int::compareTo)

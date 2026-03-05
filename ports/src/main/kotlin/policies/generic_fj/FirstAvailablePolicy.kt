package com.group7.policies.generic_fj

/**
 * First-available policy. Always selects the channel with the lowest index among available channels. Equivalent to a
 * priority policy that prefers earlier channels.
 *
 * @param ChannelT the type of channel managed by the policy
 */
class FirstAvailablePolicy<ChannelT> : PriorityPolicy<ChannelT>(Int::compareTo)

package com.group7.policies.generics

import com.group7.channels.newPullChannel
import com.group7.channels.newPullChannels
import com.group7.channels.newPushChannel
import com.group7.channels.newPushChannels
import com.group7.policies.generic_fj.FirstAvailablePolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy
import com.group7.utils.NUM_CHANNELS
import com.group7.utils.mockSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class RandomPolicyTest :
    FunSpec({
        with(mockSimulator) {
            test("RandomPolicy picks the only channel available when only one channel is open") {
                // Fork policy
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val randomForkPolicy = forkPolicy<Int>(RandomPolicy())
                randomForkPolicy.initialize(inChannel, outChannels)

                // Join policy
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val randomJoinPolicy: JoinPolicy<Int> = joinPolicy(RandomPolicy())
                randomJoinPolicy.initialize(inChannels, outChannel)
                inChannels.forEach { randomJoinPolicy.onChannelReady(it) }

                val keepOpen = 6
                check(keepOpen in 1..<NUM_CHANNELS) { "Channel to keep open must be within range of channels" }
                for (i in 0..<NUM_CHANNELS) {
                    if (i != keepOpen) {
                        randomForkPolicy.onChannelClose(outChannels[i])
                        randomJoinPolicy.onChannelNotReady(inChannels[i])
                    }
                }

                randomForkPolicy.selectChannel(SENT_OBJ) shouldBe outChannels[keepOpen]
                randomJoinPolicy.selectChannel() shouldBe inChannels[keepOpen]
            }

            test("RandomPolicy always selects from open channels") {
                // Fork policy
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailablePolicy = forkPolicy<Int>(FirstAvailablePolicy())
                firstAvailablePolicy.initialize(inChannel, outChannels)

                val open = outChannels.toMutableList()
                repeat(NUM_CHANNELS - 1) {
                    val selected = firstAvailablePolicy.selectChannel(SENT_OBJ)
                    firstAvailablePolicy.onChannelClose(selected)
                    selected shouldBeIn open

                    open.remove(selected)
                }
            }

            test("RandomForkPolicy throws when no channels are open") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailablePolicy = forkPolicy<Int>(FirstAvailablePolicy())
                firstAvailablePolicy.initialize(inChannel, outChannels)

                val open = outChannels.toMutableList()
                for (i in 0..<NUM_CHANNELS) {
                    firstAvailablePolicy.onChannelClose(outChannels[i])
                }

                shouldThrow<NoSuchElementException> { firstAvailablePolicy.selectChannel(SENT_OBJ) }
            }
        }
    })

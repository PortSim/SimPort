package com.group7.policies.fork

import com.group7.newChannel
import com.group7.newChannels
import com.group7.utils.NUM_CHANNELS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class RandomForkPolicyTest :
    FunSpec({
        test("RandomForkPolicy picks the only channel available when only one channel is open") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            val keepOpen = 6
            for (i in 0..<NUM_CHANNELS) {
                if (i != keepOpen) {
                    firstAvailablePolicy.onChannelClose(outChannels[i])
                }
            }

            firstAvailablePolicy.selectChannel(SENT_OBJ) shouldBe outChannels[keepOpen]
        }

        test("RandomForkPolicy always selects from open channels") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            val open = outChannels.toMutableList()
            for (i in 0..<NUM_CHANNELS) {
                val selected = firstAvailablePolicy.selectChannel(SENT_OBJ)
                firstAvailablePolicy.onChannelClose(selected)
                selected shouldBeIn open

                open.remove(selected)
            }
        }

        test("RandomForkPolicy throws when no channels are open") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            val open = outChannels.toMutableList()
            for (i in 0..<NUM_CHANNELS) {
                firstAvailablePolicy.onChannelClose(outChannels[i])
            }

            shouldThrow<NoSuchElementException> { firstAvailablePolicy.selectChannel(SENT_OBJ) }
        }
    })

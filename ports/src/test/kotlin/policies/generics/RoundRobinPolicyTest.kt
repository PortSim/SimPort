package com.group7.policies.generics

import com.group7.channels.newPullChannel
import com.group7.channels.newPullChannels
import com.group7.channels.newPushChannel
import com.group7.channels.newPushChannels
import com.group7.policies.generic_fj.RoundRobinPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.utils.NUM_CHANNELS
import com.group7.utils.mockSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RoundRobinPolicyTest :
    FunSpec({
        with(mockSimulator) {
            test("RoundRobinPolicy cycles through channels in order") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                for (i in 0..<NUM_CHANNELS) {
                    policy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                }
            }

            test("RoundRobinPolicy wraps around after reaching the last channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                // Exhaust one full cycle
                repeat(NUM_CHANNELS) { policy.selectChannel(SENT_OBJ) }

                // Should wrap back to channel 0
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
            }

            test("RoundRobinPolicy skips unavailable channels") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                // Close channel 0
                policy.onChannelClose(outChannels[0])
                // Should skip to channel 1
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[1]
            }

            test("RoundRobinPolicy advances pointer past closed channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                // Select channel 0, pointer advances to 1
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
                // Close channel 1
                policy.onChannelClose(outChannels[1])
                // Should skip channel 1 and pick channel 2
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[2]
            }

            test("RoundRobinPolicy resumes including reopened channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                // Close channel 0, cycle through some channels
                policy.onChannelClose(outChannels[0])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[1]

                // Reopen channel 0
                policy.onChannelOpen(outChannels[0])

                // Continue cycling — after wrapping, channel 0 should participate again
                for (i in 2..<NUM_CHANNELS) {
                    policy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                }
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
            }

            test("RoundRobinPolicy throws when all channels are closed") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                outChannels.forEach { policy.onChannelClose(it) }

                shouldThrow<NullPointerException> { policy.selectChannel(SENT_OBJ) }
            }

            test("RoundRobinPolicy distributes evenly across channels") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                val counts = IntArray(NUM_CHANNELS)
                repeat(NUM_CHANNELS * 5) {
                    val selected = policy.selectChannel(SENT_OBJ)
                    counts[outChannels.indexOf(selected)]++
                }

                counts.forEach { it shouldBe 5 }
            }

            test("RoundRobinPolicy works with join policy") {
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val policy = joinPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannels, outChannel)
                inChannels.forEach { policy.onChannelReady(it) }

                for (i in 0..<NUM_CHANNELS) {
                    policy.selectChannel() shouldBe inChannels[i]
                }
                // Wraps around
                policy.selectChannel() shouldBe inChannels[0]
            }

            test("RoundRobinPolicy handles single channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(1)
                val policy = forkPolicy<Int>(RoundRobinPolicy())
                policy.initialize(inChannel, outChannels)

                repeat(5) { policy.selectChannel(SENT_OBJ) shouldBe outChannels[0] }

                policy.onChannelClose(outChannels[0])
                shouldThrow<NullPointerException> { policy.selectChannel(SENT_OBJ) }
            }
        }
    })

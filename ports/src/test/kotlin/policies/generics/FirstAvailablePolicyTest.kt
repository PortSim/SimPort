package com.group7.policies.generics

import com.group7.channels.newPullChannel
import com.group7.channels.newPullChannels
import com.group7.channels.newPushChannel
import com.group7.channels.newPushChannels
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.FirstAvailablePolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy
import com.group7.utils.NUM_CHANNELS
import com.group7.utils.mockSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SENT_OBJ = 42

class FirstAvailablePolicyTest :
    FunSpec({
        with(mockSimulator) {
            test("FirstAvailablePolicy picks the first channel while it is open") {
                // Test fork policy version
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailableForkPolicy: ForkPolicy<Int> = forkPolicy(FirstAvailablePolicy())
                firstAvailableForkPolicy.initialize(inChannel, outChannels)

                firstAvailableForkPolicy.selectChannel(SENT_OBJ) shouldBe outChannels[0]

                // Test join policy version
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val firstAvailableJoinPolicy: JoinPolicy<Int> = joinPolicy(FirstAvailablePolicy())
                firstAvailableJoinPolicy.initialize(inChannels, outChannel)
                // Force input channels to appear as "ready", even though they aren't as they are not connected to
                // further upstream arrival nodes
                inChannels.forEach { firstAvailableJoinPolicy.onChannelReady(it) }

                firstAvailableJoinPolicy.selectChannel() shouldBe inChannels[0]
            }

            test("FirstAvailablePolicy picks the next channel when the previous closes") {
                // Fork implementation
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailableForkPolicy: ForkPolicy<Int> = forkPolicy(FirstAvailablePolicy())
                firstAvailableForkPolicy.initialize(inChannel, outChannels)

                // Join implementation
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val firstAvailableJoinPolicy: JoinPolicy<Int> = joinPolicy(FirstAvailablePolicy())
                firstAvailableJoinPolicy.initialize(inChannels, outChannel)
                inChannels.forEach { firstAvailableJoinPolicy.onChannelReady(it) }

                for (i in 0..<NUM_CHANNELS) {
                    firstAvailableForkPolicy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                    firstAvailableForkPolicy.onChannelClose(outChannels[i])

                    firstAvailableJoinPolicy.selectChannel() shouldBe inChannels[i]
                    firstAvailableJoinPolicy.onChannelNotReady(inChannels[i])
                }
            }

            test(
                "FirstAvailablePolicy picks the first channel again once onChannelOpen is called on the first channel"
            ) {
                // Fork version
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailableForkPolicy = forkPolicy<Int>(FirstAvailablePolicy())
                firstAvailableForkPolicy.initialize(inChannel, outChannels)

                // Join implementation
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val firstAvailableJoinPolicy: JoinPolicy<Int> = joinPolicy(FirstAvailablePolicy())
                firstAvailableJoinPolicy.initialize(inChannels, outChannel)
                inChannels.forEach { firstAvailableJoinPolicy.onChannelReady(it) }

                for (i in 0..<NUM_CHANNELS - 3) {
                    firstAvailableForkPolicy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                    firstAvailableForkPolicy.onChannelClose(outChannels[i])

                    firstAvailableJoinPolicy.selectChannel() shouldBe inChannels[i]
                    firstAvailableJoinPolicy.onChannelNotReady(inChannels[i])
                }

                firstAvailableForkPolicy.onChannelOpen(outChannels[0])
                firstAvailableForkPolicy.selectChannel(SENT_OBJ) shouldBe outChannels[0]

                firstAvailableJoinPolicy.onChannelReady(inChannels[0])
                firstAvailableJoinPolicy.selectChannel() shouldBe inChannels[0]
            }

            test("FirstAvailablePolicy throws when all channels are closed") {
                // Fork policy
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val firstAvailableForkPolicy = forkPolicy<Int>(FirstAvailablePolicy())
                firstAvailableForkPolicy.initialize(inChannel, outChannels)

                // Join policy
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val firstAvailableJoinPolicy: JoinPolicy<Int> = joinPolicy(FirstAvailablePolicy())
                firstAvailableJoinPolicy.initialize(inChannels, outChannel)
                inChannels.forEach { firstAvailableJoinPolicy.onChannelReady(it) }

                for (i in 0..<NUM_CHANNELS) {
                    firstAvailableForkPolicy.onChannelClose(outChannels[i])
                    firstAvailableJoinPolicy.onChannelNotReady(inChannels[i])
                }

                shouldThrow<NoSuchElementException> { firstAvailableForkPolicy.selectChannel(SENT_OBJ) }
                shouldThrow<NoSuchElementException> { firstAvailableJoinPolicy.selectChannel() }
            }
        }
    })

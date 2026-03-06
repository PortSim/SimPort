package com.group7.policies.generics

import com.group7.channels.newPullChannel
import com.group7.channels.newPullChannels
import com.group7.channels.newPushChannel
import com.group7.channels.newPushChannels
import com.group7.policies.generic_fj.PriorityPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.utils.NUM_CHANNELS
import com.group7.utils.mockSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PriorityPolicyTest :
    FunSpec({
        with(mockSimulator) {
            test("PriorityPolicy with natural ordering always selects lowest-index channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannel, outChannels)

                repeat(5) { policy.selectChannel(SENT_OBJ) shouldBe outChannels[0] }
            }

            test("PriorityPolicy with reversed ordering always selects highest-index channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(reverseOrder()))
                policy.initialize(inChannel, outChannels)

                repeat(5) { policy.selectChannel(SENT_OBJ) shouldBe outChannels[NUM_CHANNELS - 1] }
            }

            test("PriorityPolicy falls back to next best when best channel closes") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannel, outChannels)

                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
                policy.onChannelClose(outChannels[0])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[1]
                policy.onChannelClose(outChannels[1])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[2]
            }

            test("PriorityPolicy with reversed order falls back correctly") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(reverseOrder()))
                policy.initialize(inChannel, outChannels)

                policy.selectChannel(SENT_OBJ) shouldBe outChannels[NUM_CHANNELS - 1]
                policy.onChannelClose(outChannels[NUM_CHANNELS - 1])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[NUM_CHANNELS - 2]
            }

            test("PriorityPolicy reverts to original best when channel reopens") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannel, outChannels)

                policy.onChannelClose(outChannels[0])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[1]

                policy.onChannelOpen(outChannels[0])
                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
            }

            test("PriorityPolicy throws when all channels are closed") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(NUM_CHANNELS)
                val policy = forkPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannel, outChannels)

                outChannels.forEach { policy.onChannelClose(it) }

                shouldThrow<NoSuchElementException> { policy.selectChannel(SENT_OBJ) }
            }

            test("PriorityPolicy works with join policy") {
                val (_, inChannels) = newPullChannels<Int>(NUM_CHANNELS)
                val (outChannel, _) = newPullChannel<Int>()
                val policy = joinPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannels, outChannel)
                inChannels.forEach { policy.onChannelReady(it) }

                // Natural order: always picks lowest-index
                repeat(5) { policy.selectChannel() shouldBe inChannels[0] }

                policy.onChannelNotReady(inChannels[0])
                policy.selectChannel() shouldBe inChannels[1]
            }

            test("PriorityPolicy handles single channel") {
                val (_, inChannel) = newPushChannel<Int>()
                val (outChannels, _) = newPushChannels<Int>(1)
                val policy = forkPolicy<Int>(PriorityPolicy(Comparator(Int::compareTo)))
                policy.initialize(inChannel, outChannels)

                policy.selectChannel(SENT_OBJ) shouldBe outChannels[0]

                policy.onChannelClose(outChannels[0])
                shouldThrow<NoSuchElementException> { policy.selectChannel(SENT_OBJ) }
            }
        }
    })

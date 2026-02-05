package com.group7.policies.fork

import com.group7.newChannel
import com.group7.newChannels
import com.group7.utils.NUM_CHANNELS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SENT_OBJ = 42

class FirstAvailableForkPolicyTest :
    FunSpec({
        test("FirstAvailableForkPolicy picks the first channel while it is open") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            firstAvailablePolicy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
        }

        test("FirstAvailableForkPolicy picks the next channel when the previous closes") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            for (i in 0..<NUM_CHANNELS) {
                firstAvailablePolicy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                firstAvailablePolicy.onChannelClose(outChannels[i])
            }
        }

        test(
            "FirstAvailableForkPolicy picks the first channel again once onChannelOpen is called on the first channel"
        ) {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            for (i in 0..<NUM_CHANNELS - 3) {
                firstAvailablePolicy.selectChannel(SENT_OBJ) shouldBe outChannels[i]
                firstAvailablePolicy.onChannelClose(outChannels[i])
            }

            firstAvailablePolicy.onChannelOpen(outChannels[0])
            firstAvailablePolicy.selectChannel(SENT_OBJ) shouldBe outChannels[0]
        }

        test("FirstAvailableForkPolicy throws when all channels are closed") {
            val (_, inChannel) = newChannel<Int>()
            val (outChannels, _) = newChannels<Int>(NUM_CHANNELS)
            val firstAvailablePolicy = FirstAvailableForkPolicy<Int>()
            firstAvailablePolicy.initialize(inChannel, outChannels)

            for (i in 0..<NUM_CHANNELS) {
                firstAvailablePolicy.onChannelClose(outChannels[i])
            }

            shouldThrow<NoSuchElementException> { firstAvailablePolicy.selectChannel(SENT_OBJ) }
        }
    })

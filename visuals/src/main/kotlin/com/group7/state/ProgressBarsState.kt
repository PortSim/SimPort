package com.group7.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.HasProgressBars
import java.util.*
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Tracks active progress bars for delay-queue events across all node groups.
 *
 * Progress bars are created on the simulation thread via [HasProgressBars.onCreateProgressBar] callbacks and buffered.
 * On each [update] call the buffer is flushed into Compose snapshot state and expired bars are removed, all within a
 * single [Snapshot.withMutableSnapshot] to batch the writes.
 */
class ProgressBarsState(val scenario: Scenario) {
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    private var nextSequenceNumber = 0L

    /** Per-nodegroup delay queue events that are still ongoing */
    private val bufferProgressBars: Map<NodeGroup, SortedSet<ProgressBar>> =
        scenario.allNodeGroups.associateWith { node ->
            val progressBars = sortedSetOf<ProgressBar>()
            if (node is HasProgressBars) {
                node.onCreateProgressBar { label, delay ->
                    val currentTime = contextOf<Simulator>().currentTime
                    progressBars.add(ProgressBar(label, currentTime, currentTime + delay, nextSequenceNumber++))
                    expireBars(progressBars, currentTime)
                }
            }
            progressBars
        }

    private val activeProgressBars: Map<NodeGroup, MutableState<ImmutableList<ProgressBar>>> =
        scenario.allNodeGroups.associateWith { mutableStateOf(persistentListOf()) }

    fun getProgressBars(nodeGroup: NodeGroup) = activeProgressBars.getValue(nodeGroup).value

    fun update(currentTime: Instant) {
        latestTimeSeen = currentTime
        Snapshot.withMutableSnapshot {
            /* Update progress bars with latest values */
            for ((node, progressBars) in bufferProgressBars) {
                expireBars(progressBars, currentTime)
                activeProgressBars[node]?.value = progressBars.toImmutableList()
            }
        }
    }

    private fun expireBars(bars: SortedSet<ProgressBar>, currentTime: Instant) {
        val iter = bars.iterator()
        while (iter.hasNext()) {
            val bar = iter.next()
            if (bar.endTime < currentTime) {
                iter.remove()
            } else {
                break
            }
        }
    }
}

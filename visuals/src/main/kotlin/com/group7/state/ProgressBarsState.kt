package com.group7.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateSet
import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.Simulator
import com.group7.properties.DisplayProgressBars
import com.group7.properties.ProgressBar
import kotlin.time.Instant

class ProgressBarsState(val scenario: Scenario) {
    // latestTime tracks the most up-to-date time progress bars when it updates the UI should refresh
    var latestTimeSeen by mutableStateOf(Instant.DISTANT_PAST)
        private set

    /** Per-nodegroup delay queue events that are still ongoing */
    private val bufferProgressBars: Map<NodeGroup, MutableList<ProgressBar>> =
        scenario.allNodeGroups.associateWith { node ->
            val progressBars = mutableListOf<ProgressBar>()
            if (node is DisplayProgressBars) {
                node.onCreateProgressBar { label, delay ->
                    progressBars.add(
                        ProgressBar(
                            label,
                            contextOf<Simulator>().currentTime,
                            contextOf<Simulator>().currentTime + delay,
                        )
                    )
                }
            }
            progressBars
        }

    private val activeProgressBars: Map<NodeGroup, SnapshotStateSet<ProgressBar>> =
        scenario.allNodeGroups.associateWith { mutableStateSetOf() }

    fun getProgressBars(nodeGroup: NodeGroup) = activeProgressBars.getValue(nodeGroup)

    fun update(currentTime: Instant) {
        latestTimeSeen = currentTime
        Snapshot.withMutableSnapshot {
            /* Update progress bars with latest values */
            for ((node, progressBars) in bufferProgressBars) {
                activeProgressBars[node]?.addAll(progressBars)
                progressBars.clear()
            }
            for ((_, events) in activeProgressBars) {
                events.removeIf { it.endTime < latestTimeSeen }
            }
        }
    }
}

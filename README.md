# SimPort

SimPort is a Kotlin library for simulating shipping ports using queue
theory. SimPort makes designing ports fast and easy, features a clean
and powerful UI, and runs fast at ==TKTK== events per second with metrics.

Detailed documentation (including a user guide and examples) is available
at ==TKTK==.

## 🚀 Features

- Design ports in-code with out clean domain specific language
- Compare arbitrary parameters or even different designs
- Create custom nodes or subnetworks using simple component nodes
- Build your own node logic with custom policies
- See node metrics in our simple UI
- Supported anywhere you can write Kotlin!

<!-- - Export metrics with the click of a button -->

## Usage

Building a `scenario` (port design) is as simple as:

```kotlin
val scenario = buildScenario {
    // Create and an arrivals node
    arrivals(
        // Name:
        "Truck Arrivals",
        // Define a generator to emit trucks:
        Generators.constant(Truck, Delays.exponential(truckArrivalsPerHour, DurationUnit.HOURS)).let {
            if (numTrucks != null) it.take(numTrucks) else it
        },
    )
        // Add an unbounded queue
        .thenQueue("Truck Arrival Queue")
        // Add a sink for the trucks to reach
        .thenSink("Truck Departures")
}
```

Then run a simulation like this:

```kotlin
// Create a sampler to store the metrics we want
val sampler = MetricsPanelState(scenario, 5.minutes)
// Create a simulator
val simulator = Simulator(EventLog.noop(), scenario, sampler)
// EventLog.noop() does nothing, but you could replace it
// Run the simulation for however long you'd like
sampler.beginBatch()
simulator.runFor(3.days)
sampler.endBatch()
```

You can run
a [live visualisation](https://gitlab.doc.ic.ac.uk/g256002107/simport/-/blob/readme/visuals/src/main/kotlin/LiveVisualisation.kt),
where you see the simulation run
in realtime,
a [static visualisation](https://gitlab.doc.ic.ac.uk/g256002107/simport/-/blob/readme/visuals/src/main/kotlin/StaticVisualisation.kt),
where the simulation is run in
advance, or
a [multi visualisation](https://gitlab.doc.ic.ac.uk/g256002107/simport/-/blob/readme/visuals/src/main/kotlin/MultiVisualisation.kt),
for parameter sweeps.

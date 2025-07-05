package simulator.simulations.validation;

import simulator.SimulationRunner;
import simulator.regions.BrokerWithRegionProcessingLocation;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for simulations that are designed to validate
 * a topology generator or test the core correctness of the system, rather than
 * running large-scale performance experiments.
 * <p>
 * This class primarily serves as a common ancestor to group validation-style
 * simulations. The core logic remains in the `executeScenarios` method
 * of the concrete subclasses, as each validation test is unique.
 *
 * @param <C> The specific type of TopologyConfiguration.
 * @param <F> The specific type of AbstractTopologyFactory.
 */
public abstract class AbstractTopologyValidationSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegionProcessingLocation>
> extends SimulationRunner<C, BrokerWithRegionProcessingLocation, F> {
    // This class is intentionally left empty as its main purpose is to provide
    // a common type for validation-focused simulations.
}

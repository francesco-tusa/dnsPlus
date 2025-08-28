package simulator.simulations.validation;

import java.util.function.Predicate;
import simulator.SimulationRunner;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * A generic, configurable class for running a validation simulation.
 * It takes a validation test (as a Predicate) and runs it on a generated topology.
 *
 * @param <C> The specific type of TopologyConfiguration for the simulation.
 * @param <R> The specific type of the root node, which must be a BrokerWithRegion.
 * @param <F> The specific type of AbstractTopologyFactory that produces the topology.
 */
public class ConfigurableValidationSimulation<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>
> extends SimulationRunner<C, R, F> {

    private final Predicate<R> validationTest;
    private final String validationTestName;

    /**
     * @param validationTest A lambda expression or Predicate that contains the validation logic.
     * It takes the root of the topology (R) and returns true if validation passes.
     * @param validationTestName A descriptive name for the test being run.
     */
    public ConfigurableValidationSimulation(Predicate<R> validationTest, String validationTestName) {
        this.validationTest = validationTest;
        this.validationTestName = validationTestName;
    }

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Executing Validation Scenario: " + this.validationTestName + " ---");
        if (this.rootNode == null) {
            System.err.println("Validation failed: Root node is null.");
            return;
        }

        boolean success = validationTest.test(this.rootNode);

        System.out.println("\n--- Validation Result ---");
        if (success) {
            System.out.println("SUCCESS: The '" + this.validationTestName + "' check passed.");
        } else {
            System.out.println("FAILED: The '" + this.validationTestName + "' check did not pass.");
        }
    }
}
package simulator.simulations.validation;

import java.util.function.Predicate;
import simulator.VisualisedSimulationRunner; // Changed import
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * A generic, configurable class for running a validation simulation.
 * It now extends VisualisedSimulationRunner to automatically include the GUI.
 */
public class ConfigurableValidationSimulation<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>
> extends VisualisedSimulationRunner<C, R, F> {

    private final Predicate<R> validationTest;
    private final String validationTestName;

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
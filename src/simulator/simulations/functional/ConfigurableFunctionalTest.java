package simulator.simulations.functional;

import java.util.function.Predicate;
import java.util.logging.Level;
import simulator.core.VisualisedSimulationRunner;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

public class ConfigurableFunctionalTest<
    C extends TopologyConfiguration,
    R extends BrokerWithRegion,
    F extends AbstractTopologyFactory<C, R>
> extends VisualisedSimulationRunner<C, R, F> {

    private final Predicate<R> validationTest;
    private final String validationTestName;

    public ConfigurableFunctionalTest(Predicate<R> validationTest, String validationTestName) {
        this.validationTest = validationTest;
        this.validationTestName = validationTestName;
    }

    @Override
    protected Level getLogLevel() {
        return Level.FINE;
    }
    
    /**
     * For functional tests, the setup involves attaching the hardcoded clients
     * from the topology generator.
     */
    @Override
    protected void setupSimulation() {
        System.out.println("\n--- Attaching clients for functional test ---");
        topologyFactory.attachSubscribers(this.rootNode);
        topologyFactory.attachPublishers(this.rootNode);
    }

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Executing Functional Test Scenario: " + this.validationTestName + " ---");
        if (this.rootNode == null) {
            System.err.println("Functional test failed: Root node is null.");
            return;
        }

        boolean success = validationTest.test(this.rootNode);

        System.out.println("\n--- Functional Test Result ---");
        if (success) {
            System.out.println("SUCCESS: The '" + this.validationTestName + "' check passed.");
        } else {
            System.out.println("FAILED: The '" + this.validationTestName + "' check did not pass.");
        }
    }
}
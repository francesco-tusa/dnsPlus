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
     * Overrides the new hook method. It first attaches the clients needed for the
     * functional test, and then calls the parent method to trigger the visualization.
     */
    @Override
    protected void attachClientsAndVisualize() {
        System.out.println("\n--- Attaching clients for functional test ---");
        topologyFactory.attachSubscribers(this.rootNode);
        topologyFactory.attachPublishers(this.rootNode);
        
        // After clients are attached, call the parent to draw the graph.
        super.attachClientsAndVisualize();
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
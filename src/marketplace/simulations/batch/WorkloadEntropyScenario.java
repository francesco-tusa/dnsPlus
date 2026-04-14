package marketplace.simulations.batch;

import java.util.Properties;
import java.util.function.Supplier;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class WorkloadEntropyScenario extends AbstractMarketplaceScenario {

    public WorkloadEntropyScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        // This explicitly targets the property set by the Orchestrator's 
        // clientPopulations loop, overwriting the default value safely.
        return "workload.replicas"; 
    }

    @Override
    public String[] getKnobValues() {
        // Sweeping from a light load up to a massive load to track HE degradation
        return new String[] {"10000", "50000", "100000", "500000"};
    }

    @Override
    protected void configureSpecific(Properties props) {
        super.configureSpecific(props);
        // We ensure the arrival distribution acts predictably to calculate total theoretical requests
        props.setProperty("workload.arrivalDistribution", "UNIFORM");
        props.setProperty("workload.meanSubscriptionsPerSubscriber", "1");
    }
}
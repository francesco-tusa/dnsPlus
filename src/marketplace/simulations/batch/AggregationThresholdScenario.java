package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class AggregationThresholdScenario extends AbstractMarketplaceScenario {

    public AggregationThresholdScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        return "broker.smartThreshold";
    }

    @Override
    public String[] getKnobValues() {
        //return new String[] { "0.0", "0.05", "0.10", "0.15", "0.20", "0.25", "0.30", "0.40", "0.50", "0.60", "0.75", "1.00" };
        return new String[] { "0.0", "0.10", "0.20", "0.30", "0.40", "0.50", "0.60", "0.7", "0.8", "0.9", "1.00" };
    }
}
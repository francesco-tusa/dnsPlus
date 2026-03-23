package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.MarketplaceContinuumSimulation;

public class EdgeProbabilityScenario extends AbstractMarketplaceScenario {

    // Inject the simulation engine (Legacy or Azure) via the constructor
    public EdgeProbabilityScenario(Supplier<MarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        return "marketplace.workload.edge.probability";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "0.0", "0.10", "0.30", "0.50", "0.70", "0.90", "1.0" };
    }
}
package marketplace.simulations.batch;

import java.util.function.Supplier;

import marketplace.simulations.MarketplaceContinuumSimulation;

/**
 * Sweeps the probability of a client request having a strict ($10) budget constraint.
 * Used to demonstrate the optimality gap of Geographic DNS (Baseline) when
 * dealing with multi-objective QoS constraints in a Cloud-Only topology.
 */
public class BudgetProbabilityScenario extends AbstractMarketplaceScenario {

    public BudgetProbabilityScenario(Supplier<MarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        return "marketplace.workload.strict_budget.probability";
    }

    @Override
    public String[] getKnobValues() {
        // Sweeping from 0% strict budget requests up to 100%
        return new String[] { "0.0", "0.10", "0.30", "0.50", "0.70", "0.90", "1.0"};
    }
}
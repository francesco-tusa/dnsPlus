package marketplace.simulations.batch;

import java.util.function.Supplier;
import marketplace.simulations.MarketplaceContinuumSimulation;

public enum ExperimentType {

    // --- Original Scenarios ---
    EDGE_WORKLOAD_SWEEP("edge_workload_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new EdgeProbabilityScenario(factory);
        }
    },
    AGGREGATION_SWEEP("aggregation_threshold_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new AggregationThresholdScenario(factory);
        }
    },
    BUDGET_SENSITIVITY_SWEEP("budget_sensitivity_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new BudgetProbabilityScenario(factory);
        }
    },

    // --- New HE Feasibility & Physics Scenarios ---
    HE_TOPOLOGY_SCALE_SWEEP("he_topology_scale_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new TopologyScaleScenario(factory);
        }
    },
    HE_TENANT_CAPACITY_SWEEP("he_tenant_capacity_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new TenantCapacityScenario(factory);
        }
    },
    HE_WORKLOAD_ENTROPY_SWEEP("he_workload_entropy_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new WorkloadEntropyScenario(factory);
        }
    },
    HE_TRACE_SCALE_SWEEP("he_trace_scale_results_") {
        @Override
        public AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory) {
            return new TraceScaleScenario(factory);
        }
    };

    private final String baseCsvFilename;

    ExperimentType(String baseCsvFilename) {
        this.baseCsvFilename = baseCsvFilename;
    }

    public String getBaseCsvFilename() {
        return baseCsvFilename;
    }

    /**
     * Polymorphic factory method to instantiate the correct scenario safely.
     */
    public abstract AbstractMarketplaceScenario createScenario(Supplier<MarketplaceContinuumSimulation> factory);
}
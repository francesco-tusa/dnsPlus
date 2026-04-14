package marketplace.simulations.batch;

import java.util.Properties;
import java.util.function.Supplier;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class TraceScaleScenario extends AbstractMarketplaceScenario {

    public TraceScaleScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        return "marketplace.workload.traces.limit"; 
    }

    @Override
    public String[] getKnobValues() {
        // Sweeping from 1K traces up to 1M traces
        return new String[] {"1000", "10000", "100000", "1000000"};
    }

    @Override
    protected void configureSpecific(Properties props) {
        super.configureSpecific(props);
        
        // Ensure the Cloud anchor can hold the largest possible sweep value (1M).
        // If Cloud capacity < Traces, the functions disappear from the simulation entirely.
        props.setProperty("marketplace.tenant.capacity.cloud", "1000000");
        
        // Keep Fog and Edge strictly constrained to observe the "Long Tail Edge Bypass" effect
        props.setProperty("marketplace.tenant.capacity.fog", "100");
        props.setProperty("marketplace.tenant.capacity.edge", "10");
    }
}
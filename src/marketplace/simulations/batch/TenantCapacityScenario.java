package marketplace.simulations.batch;

import java.util.Properties;
import java.util.function.Supplier;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class TenantCapacityScenario extends AbstractMarketplaceScenario {

    public TenantCapacityScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        return "marketplace.tenant.capacity.edge";
    }

    @Override
    public String[] getKnobValues() {
        //return new String[] {"2", "5", "10", "25", "50", "100"};
        return new String[] {"50", "100"};
    }

    @Override
    protected void configureSpecific(Properties props) {
        super.configureSpecific(props);
        // Additional strategy-specific overrides can be added here if needed
    }
}
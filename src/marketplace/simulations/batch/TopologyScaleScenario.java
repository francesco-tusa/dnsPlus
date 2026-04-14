package marketplace.simulations.batch;

import java.util.Properties;
import java.util.function.Supplier;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;

public class TopologyScaleScenario extends AbstractMarketplaceScenario {

    public TopologyScaleScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        super(simulationFactory);
    }

    @Override
    public String getKnobKey() {
        // Sweeping the edge forces multidimensional spatial expansion
        return "marketplace.providers.edge.count";
    }

    @Override
    public String[] getKnobValues() {
        // Represents topologies of: {10/100/1000}, {25/250/2500}, {50/500/5000}, {100/1000/10000}
        return new String[] {"1000", "2500", "5000", "10000"};
    }

    @Override
    protected void configureSpecific(Properties props) {
        super.configureSpecific(props);
        
        // The orchestrator injects the swept edge count via getKnobKey().
        // We intercept it to scale the fog and cloud tiers proportionally (10:1 ratio).
        int edgeNodes = Integer.parseInt(props.getProperty(getKnobKey(), "1000"));
        int fogNodes = Math.max(1, edgeNodes / 10);
        int cloudNodes = Math.max(1, fogNodes / 10);
        
        props.setProperty("marketplace.providers.cloud.count", String.valueOf(cloudNodes));
        props.setProperty("marketplace.providers.fog.count", String.valueOf(fogNodes));
    }
}
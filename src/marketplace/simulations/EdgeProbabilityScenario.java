package marketplace.simulations;

public class EdgeProbabilityScenario extends AbstractMarketplaceScenario {

    @Override
    public String getKnobKey() {
        return "marketplace.workload.edge.probability";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "0.10", "0.30", "0.50", "0.70", "0.90" };
    }
}
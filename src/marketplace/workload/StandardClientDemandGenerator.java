package marketplace.workload;

import java.util.Map;
import java.util.Random;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;

public class StandardClientDemandGenerator implements ClientDemandGenerator {
    private final Random random;

    public StandardClientDemandGenerator(Random seededRandom) { this.random = seededRandom; }

    @Override
    public ClientDemandProfile generateDemand(int clientIndex, long functionId) {
        // Blind to functionId. Hardcodes SLA probabilities.
        double edgeProb = MarketplaceConfig.get().edgeWorkloadProbability;
        int classType = (random.nextDouble() < edgeProb) ? 0 : 1; 

        if (classType == 0) {
            return new ClientDemandProfile(
                Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 30.0, MarketplaceMetricSchema.METRIC_COST, 150.0, MarketplaceMetricSchema.METRIC_RELIABILITY, 0.90, MarketplaceMetricSchema.METRIC_BANDWIDTH, 15.0),
                Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 0.80, MarketplaceMetricSchema.METRIC_COST, 0.05, MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10, MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05)
            );
        } else {
            return new ClientDemandProfile(
                Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 500.0, MarketplaceMetricSchema.METRIC_COST, 80.0, MarketplaceMetricSchema.METRIC_RELIABILITY, 0.99, MarketplaceMetricSchema.METRIC_BANDWIDTH, 100.0),
                Map.of(MarketplaceMetricSchema.METRIC_LATENCY, 0.05, MarketplaceMetricSchema.METRIC_COST, 0.80, MarketplaceMetricSchema.METRIC_RELIABILITY, 0.10, MarketplaceMetricSchema.METRIC_BANDWIDTH, 0.05)
            );
        }
    }
}
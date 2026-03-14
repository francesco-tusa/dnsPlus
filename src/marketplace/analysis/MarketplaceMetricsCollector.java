package marketplace.analysis;

import java.util.List;
import java.util.Map;

import marketplace.agents.MarketplaceProvider;
import marketplace.events.ServiceRequest;
import marketplace.optimization.WeightedUtilityStrategy;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.simulations.performance.metrics.MetricsCollector;
import simulator.simulations.performance.metrics.PerformanceMetricsData;

public class MarketplaceMetricsCollector extends MetricsCollector {

    private final MarketplaceGroundTruthCalculator oracle;
    private final WeightedUtilityStrategy strategy = new WeightedUtilityStrategy();

    public MarketplaceMetricsCollector(MarketplaceGroundTruthCalculator oracle) {
        this.oracle = oracle;
    }

    @Override
    public PerformanceMetricsData collect(TreeNode root, List<SubscriberWithLocation> subs, List<PublisherWithLocation> pubs) {
        
        MarketplacePerformanceMetricsData marketData = new MarketplacePerformanceMetricsData();
        super.populateMetrics(root, subs, pubs, marketData);

        Map<Long, Double> optimalScores = oracle.getOracleOptimalScores();
        marketData.groundTruthMatches = optimalScores.size();

        for (SubscriberWithLocation sub : subs) {
            if (!(sub instanceof MarketplaceProvider provider)) continue;
            
            for (ServiceRequest req : provider.getDeliveredRequests()) {
                Double optimalScore = optimalScores.get(req.getOriginalRequestId());
                var actualResult = strategy.inspect(provider.getLastAdvertisedOffer(), req);

                if (optimalScore == null) {
                    marketData.slaViolations++;
                    marketData.unfeasibleSlaViolations++;
                    continue;
                }

                if (!actualResult.isFeasible()) {
                    marketData.slaViolations++;
                    marketData.feasibleSlaViolations++;

                    // Read the actual raw score directly from the result! No recalculation needed.
                    marketData.cumulativeDeliveredUtility += actualResult.score();
                    double slaGap = (actualResult.score() - optimalScore) / optimalScore;
                    marketData.cumulativeSlaViolationDegradation += slaGap;
                    continue;
                }

                // Accumulate standard score for feasible deliveries
                marketData.cumulativeDeliveredUtility += actualResult.score();
                
                // CATEGORY 3 & 4: Feasible Deliveries (Optimal vs Suboptimal)
                if (Math.abs(actualResult.score() - optimalScore) < 1e-5) {
                    marketData.optimalDeliveries++;
                } else {
                    marketData.suboptimalDeliveries++;
                    double optimalityGap = (actualResult.score() - optimalScore) / optimalScore;
                    marketData.cumulativeUtilityDegradation += optimalityGap;
                }
            }
        }

        return marketData;
    }
}
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
                if (optimalScore == null) continue; 

                // Compute the utility score of the actual provider chosen by the decentralized network
                var actualResult = strategy.inspect(provider.getLastAdvertisedOffer(), req);
                
                // Accumulate Absolute Utility
                marketData.cumulativeDeliveredUtility += actualResult.score();
                
                if (!actualResult.isFeasible()) {
                    marketData.slaViolations++;
                }
                
                // Floating point epsilon comparison (1e-5)
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
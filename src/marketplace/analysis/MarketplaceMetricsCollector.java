package marketplace.analysis;

import java.util.IntSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import marketplace.agents.AbstractMarketplaceBroker;
import marketplace.agents.MarketplaceProvider;
import marketplace.events.ServiceRequest;
import marketplace.optimization.WeightedUtilityStrategy;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
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
        
        // 1. Run standard global collection
        super.populateMetrics(root, subs, pubs, marketData);

        // 2. TIER-BY-TIER TOPOLOGY TRAVERSAL (State Analytics)
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();
            
            if (curr instanceof SimulationBroker broker) {
                // Calculate precise topological level (0 = Root, increasing downwards)
                int level = 0;
                TreeNode pointer = broker;
                while (pointer.getParent() != null) {
                    level++;
                    pointer = pointer.getParent();
                }
                
                // Track Spatial Index Size (Total Bounding Boxes / MetricHyperCubes)
                marketData.tierSpatialIndexStats
                    .computeIfAbsent(level, k -> new IntSummaryStatistics())
                    .accept(broker.getInputSubscriptionCount());

                // Track Function Directory Size (Unique Topics / HE Envelopes)
                if (broker instanceof AbstractMarketplaceBroker mb) {
                    marketData.tierFunctionDirectoryStats
                        .computeIfAbsent(level, k -> new IntSummaryStatistics())
                        .accept(mb.getFunctionDirectorySize());
                }
            }
            if (curr.getChildren() != null) queue.addAll(curr.getChildren());
        }

        // 3. MULTI-OBJECTIVE ORACLE COMPARISON (Routing Accuracy Analytics)
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

                    // Request was misrouted to a node that breaches the SLA bounds
                    marketData.cumulativeDeliveredUtility += actualResult.score();
                    double slaGap = (actualResult.score() - optimalScore) / optimalScore;
                    marketData.cumulativeSlaViolationDegradation += slaGap;
                    continue;
                }

                // Accumulate standard score for mathematically feasible deliveries
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
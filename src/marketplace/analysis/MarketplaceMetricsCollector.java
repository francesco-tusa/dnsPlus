package marketplace.analysis;

import java.util.IntSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import marketplace.agents.AbstractMarketplaceBroker;
import marketplace.agents.MarketplaceProvider;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.optimization.WeightedUtilityStrategy;
import marketplace.optimization.WeightedUtilityStrategy.EvaluationResult;
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
        
        super.populateMetrics(root, subs, pubs, marketData);

        // 1. TIER-BY-TIER TOPOLOGY TRAVERSAL (State Analytics)
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();
            
            if (curr instanceof SimulationBroker broker) {
                int level = 0;
                TreeNode pointer = broker;
                while (pointer.getParent() != null) {
                    level++;
                    pointer = pointer.getParent();
                }
                
                marketData.tierSpatialIndexStats
                    .computeIfAbsent(level, k -> new IntSummaryStatistics())
                    .accept(broker.getInputSubscriptionCount());

                if (broker instanceof AbstractMarketplaceBroker mb) {
                    marketData.tierFunctionDirectoryStats
                        .computeIfAbsent(level, k -> new IntSummaryStatistics())
                        .accept(mb.getFunctionDirectorySize());
                    
                        // Aggregating the HE tracking variables
                    marketData.totalIdLinearComputations += mb.getTotalIdLinearComputations();
                    marketData.totalIdBstComputations += mb.getTotalIdBstComputations();    
                }
            }
            if (curr.getChildren() != null) queue.addAll(curr.getChildren());
        }

        // 2. MULTI-OBJECTIVE ORACLE COMPARISON (Routing Accuracy Analytics)
        Map<Long, Double> optimalScores = oracle.getOracleOptimalScores();
        marketData.groundTruthMatches = optimalScores.size();

        for (SubscriberWithLocation sub : subs) {
            if (!(sub instanceof MarketplaceProvider provider)) continue;
            
            for (ServiceRequest req : provider.getDeliveredRequests()) {
                Double optimalScore = optimalScores.get(req.getOriginalRequestId());
                
                // Retrieve the precise SLA profile for this function
                ServiceOffer executedOffer = provider.getSpecificOffer(req.getOracleServiceId());
                
                if (executedOffer == null) {
                    marketData.slaViolations++;
                    marketData.unfeasibleSlaViolations++;
                    continue;
                }

                EvaluationResult actualResult = strategy.inspect(executedOffer, req);

                if (optimalScore == null) {
                    // CATEGORY 1: Pure False Positive. The GT knew no provider could satisfy this SLA, 
                    // but the broker's HyperCube aggregation falsely advertised a mathematical overlap.
                    marketData.slaViolations++;
                    marketData.unfeasibleSlaViolations++;
                    continue;
                }

                if (!actualResult.isFeasible()) {
                    // CATEGORY 2: Misrouting. A valid provider existed elsewhere, but the network 
                    // delivered it to this node which violates the strict constraints at the endpoint.
                    marketData.slaViolations++;
                    marketData.feasibleSlaViolations++;

                    marketData.cumulativeDeliveredUtility += actualResult.score();
                    double slaGap = (actualResult.score() - optimalScore) / optimalScore;
                    marketData.cumulativeSlaViolationDegradation += slaGap;
                    continue;
                }
                
                marketData.cumulativeDeliveredUtility += actualResult.score();
                
                // CATEGORY 3 & 4: Successful Deliveries (Optimal vs Suboptimal)
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
package marketplace.analysis;

import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import marketplace.agents.AbstractMarketplaceBroker;
import marketplace.agents.MarketplaceProvider;
import marketplace.common.MarketplaceMetricSchema;
import marketplace.config.MarketplaceConfig;
import marketplace.events.ServiceOffer;
import marketplace.events.ServiceRequest;
import marketplace.optimization.TelemetryLoggingStrategy;
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
                    
                    marketData.totalIdLinearComputations += mb.getTotalIdLinearComputations();
                    marketData.totalIdBstComputations += mb.getTotalIdBstComputations();    
                }
            }
            if (curr.getChildren() != null) queue.addAll(curr.getChildren());
        }

        // 2. MULTI-OBJECTIVE ORACLE COMPARISON (Routing Accuracy Analytics)
        Map<Long, Double> optimalScores = oracle.getOracleOptimalScores();
        marketData.groundTruthMatches = optimalScores.size();

        Map<Long, Map<String, Object>> globalTrajectoryOutcomes = new HashMap<>();
        
        int unfeasibleGroundTruthCount = 0;

        // Initialize every request that had a valid Ground Truth solution
        for (Map.Entry<Long, Double> entry : optimalScores.entrySet()) {
            // Count requests that the Oracle proved were impossible to satisfy anywhere
            if (entry.getValue() == 99999.0) {
                unfeasibleGroundTruthCount++;
                continue; 
            }
            
            Map<String, Object> record = new HashMap<>();
            record.put("originalRequestId", entry.getKey());
            record.put("optimalScore", entry.getValue());
            record.put("deliveredScore", null); 
            // Default assumption: It had a valid solution, but the network dropped it
            record.put("status", "DROPPED_FEASIBLE"); 
            
            globalTrajectoryOutcomes.put(entry.getKey(), record);
        }
        
        marketData.unfeasibleSlaViolations = unfeasibleGroundTruthCount;

        // 3. EVALUATE ACTUAL DELIVERIES
        for (SubscriberWithLocation sub : subs) {
            // Strictly adhere to MarketplaceProvider casting
            if (!(sub instanceof MarketplaceProvider provider)) continue;
            
            // Strictly use your custom tracked list
            for (ServiceRequest req : provider.getDeliveredRequests()) {
                long reqId = req.getOriginalRequestId();
                Double optimalScore = optimalScores.get(reqId);
                ServiceOffer executedOffer = provider.getSpecificOffer(req.getOracleServiceId());
                
                Map<String, Object> record = globalTrajectoryOutcomes.computeIfAbsent(reqId, k -> new HashMap<>());
                record.put("originalRequestId", reqId);

                if (executedOffer == null || optimalScore == null || optimalScore == 99999.0) {
                    record.put("status", "UNFEASIBLE_SLA_VIOLATION");
                    continue;
                }

                EvaluationResult actualResult = strategy.inspect(executedOffer, req);
                record.put("deliveredScore", actualResult.score());

                // MORL RAW METRICS EXTRACTION
                record.put("finalProviderNode", provider.getName());
                
                Map<String, Double> yields = executedOffer.getQosMetrics();
                if (yields != null) {
                    // 1. Extract and store all base metrics
                    for (Map.Entry<String, Double> yieldEntry : yields.entrySet()) {
                        record.put("actual_" + yieldEntry.getKey(), yieldEntry.getValue());
                    }

                    // 2. Calculate network distance and delay
                    double distance = Math.sqrt(req.getLocation().distanceSquared(provider.getLocation()));
                    double netLatency = distance * MarketplaceMetricSchema.DISTANCE_TO_TIME_FACTOR;
                    
                    record.put("network_distance", distance);
                    record.put("network_latency", netLatency);

                    // 3. Safely retrieve the Double before doing arithmetic
                    Double baseLatency = yields.get(MarketplaceMetricSchema.METRIC_LATENCY);
                    
                    // Only perform the addition if baseLatency is not null
                    if (baseLatency != null) {
                        record.put("actual_end_to_end_latency", baseLatency + netLatency);
                    }
                }

                // Request arrived, but the SLA was violated at the leaf
                if (!actualResult.isFeasible()) {
                    record.put("status", "FEASIBLE_SLA_VIOLATION");
                    marketData.feasibleSlaViolations++;
                    marketData.cumulativeDeliveredUtility += actualResult.score();
                    marketData.cumulativeSlaViolationDegradation += (actualResult.score() - optimalScore) / optimalScore;
                    continue;
                }
                
                marketData.cumulativeDeliveredUtility += actualResult.score();
                
                // Compare to Oracle for Optimality
                if (Math.abs(actualResult.score() - optimalScore) < 1e-5) {
                    record.put("status", "OPTIMAL");
                    marketData.optimalDeliveries++;
                } else {
                    record.put("status", "SUBOPTIMAL");
                    marketData.suboptimalDeliveries++;
                    marketData.cumulativeUtilityDegradation += (actualResult.score() - optimalScore) / optimalScore;
                }
            }
        }

        // RESTORED DEAD END CALCULATION
        int totalPossibleFeasible = optimalScores.size() - unfeasibleGroundTruthCount;
        int deadEnds = totalPossibleFeasible - (int)(marketData.optimalDeliveries + marketData.suboptimalDeliveries + marketData.feasibleSlaViolations);
        
        if (deadEnds > 0) {
            marketData.feasibleSlaViolations += deadEnds;
        }
        
        marketData.slaViolations = marketData.feasibleSlaViolations + marketData.unfeasibleSlaViolations;

        // 4. DELEGATE FLUSHING TO THE DECENTRALIZED BROKERS
        if (MarketplaceConfig.get().collectFlTelemetry) {
            TelemetryLoggingStrategy.flushLocalizedOutcomes(globalTrajectoryOutcomes);
        }

        return marketData;
    }
}
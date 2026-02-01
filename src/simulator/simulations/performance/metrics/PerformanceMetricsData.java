package simulator.simulations.performance.metrics;

import java.util.IntSummaryStatistics;

public abstract class PerformanceMetricsData {
    
    // --- Common: Broker State (Memory) ---
    public final IntSummaryStatistics inputTableStats = new IntSummaryStatistics();
    public final IntSummaryStatistics outputTableStats = new IntSummaryStatistics();
    public final IntSummaryStatistics coreInputTableStats = new IntSummaryStatistics();
    
    // --- Topology Stats ---
    public long totalBrokers = 0;
    public long totalLeafBrokers = 0;

    // --- Common: Traffic & Processing Load ---
    public long totalSubscriptionInputEvents = 0;
    public long totalPublicationsSent = 0;
    public long totalPublicationProcessingEvents = 0;
    public long totalMatchingComputations = 0;
    public long totalPublicationsForwarded = 0;

    // --- Common: Delivery & Accuracy ---
    public long totalDeliveriesReceived = 0;
    public long totalFalsePositiveEvents = 0;
    public long totalFalsePositiveDeliveries = 0;
    
    // --- Common: Hops (Network Latency Proxy) ---
    public long totalHopSum = 0;
    public int globalMinHops = Integer.MAX_VALUE;
    public int globalMaxHops = Integer.MIN_VALUE;
    
    // --- Ground Truth ---
    public long groundTruthMatches = 0; 
    
    public abstract String getAlgorithmLabel();
}
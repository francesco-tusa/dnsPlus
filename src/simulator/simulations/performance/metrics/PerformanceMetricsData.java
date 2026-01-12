package simulator.simulations.performance.metrics;

import java.util.IntSummaryStatistics;

public abstract class PerformanceMetricsData {
    
    // --- Common: Subscription State ---
    public final IntSummaryStatistics inputTableStats = new IntSummaryStatistics();  
    public final IntSummaryStatistics outputTableStats = new IntSummaryStatistics(); 
    public long totalSubscriptionTraffic = 0;

    // --- Common: Traffic & Cost ---
    public long totalPubsSent = 0;
    public long totalPubForwardingEvents = 0; 
    public long totalMatchingComputations = 0; 

    // --- Common: Delivery & Accuracy ---
    public long totalNotifications = 0;
    public long totalFalsePositiveEvents = 0; // At Broker level
    public long totalFalsePositiveDeliveries = 0; // At Subscriber level
    
    // --- Common: Hops ---
    public long totalHopSum = 0;
    public long totalHopCount = 0;
    public int globalMinHops = Integer.MAX_VALUE;
    public int globalMaxHops = Integer.MIN_VALUE;
    
    // --- Ground Truth ---
    public long groundTruthMatches = 0; 
    
    // Polymorphic Label
    public abstract String getAlgorithmLabel();
}
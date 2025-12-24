package simulator.simulations.performance.metrics;
import java.util.IntSummaryStatistics;

public class PerformanceMetricsData {
    // 1. Subscription State
    public final IntSummaryStatistics inputTableStats = new IntSummaryStatistics();  
    public final IntSummaryStatistics outputTableStats = new IntSummaryStatistics(); 

    // 2. Processing Logic (INPUT)
    public long totalSubCovered = 0;
    public long totalSubExpanded = 0;
    public long totalSubAdded = 0;
    public long totalSubAbsorbed = 0;
    public long totalSubMerged = 0;

    // 2b. Processing Logic (OUTPUT)
    public long totalOutSubCovered = 0;
    public long totalOutSubExpanded = 0;
    public long totalOutSubAdded = 0;
    public long totalOutSubAbsorbed = 0;
    public long totalOutSubMerged = 0;

    public long totalSubscriptionTraffic = 0;

    // 3. Traffic & Cost
    public long totalPubsSent = 0;
    public long totalPubForwardingEvents = 0; 
    public long totalMatchingComputations = 0; 

    // 4. Delivery
    public long totalNotifications = 0;
    public final IntSummaryStatistics hopStats = new IntSummaryStatistics();
    
    // 5. Accuracy / Routing Efficiency
    public long groundTruthMatches = 0;
    public long totalFalsePositiveEvents = 0;      // Dead Ends (Broker Routing Errors)
    public long totalFalsePositiveDeliveries = 0;  // Delivered but unwanted (Geometric Errors)
}
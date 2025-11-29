package simulator.simulations.performance.metrics;
import java.util.IntSummaryStatistics;

public class PerformanceMetricsData {
    // 1. Subscription State
    public final IntSummaryStatistics inputTableStats = new IntSummaryStatistics();  
    public final IntSummaryStatistics outputTableStats = new IntSummaryStatistics(); 

    // 2. Processing Logic
    public long totalSubCovered = 0;
    public long totalSubExpanded = 0;
    public long totalSubAdded = 0;
    public long totalSubscriptionTraffic = 0;

    // 3. Traffic & Cost
    public long totalPubsSent = 0;
    public long totalPubForwardingEvents = 0; 
    public long totalMatchingComputations = 0; 
    public long totalFalsePositiveEvents = 0;

    // 4. Delivery
    public long totalNotifications = 0;
    public final IntSummaryStatistics hopStats = new IntSummaryStatistics();
    
    // 5. Accuracy
    public long groundTruthMatches = 0;
}
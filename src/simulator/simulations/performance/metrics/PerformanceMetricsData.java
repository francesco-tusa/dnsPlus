package simulator.simulations.performance.metrics;
import java.util.IntSummaryStatistics;

public class PerformanceMetricsData {
    public final IntSummaryStatistics tableSizeStats = new IntSummaryStatistics();
    public long totalSubCovered = 0;
    public long totalSubExpanded = 0;
    public long totalSubAdded = 0;
    public long totalSubscriptionTraffic = 0;

    public long totalPubsSent = 0;
    public long totalPubForwardingEvents = 0; 
    public long totalMatchingComputations = 0;

    public long totalFalsePositiveEvents = 0;

    public long totalNotifications = 0;
    public final IntSummaryStatistics hopStats = new IntSummaryStatistics();
    public long groundTruthMatches = 0;
}
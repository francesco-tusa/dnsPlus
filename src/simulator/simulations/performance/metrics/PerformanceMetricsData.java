package simulator.simulations.performance.metrics;

import java.util.IntSummaryStatistics;

/**
 * Abstract base class for collecting performance metrics across different simulation architectures.
 * This class defines the common metrics applicable to both Regional (Spatial Match) and
 * Proximity (Closest Node) simulations, such as traffic volume, computational cost, and delivery accuracy.
 */
public abstract class PerformanceMetricsData {
    
    // --- Common: Broker State (Memory) ---

    /**
     * Statistics (Min, Max, Avg, Count) for the size of the "Input Table" (Subscriptions received).
     * Represents the number of children/subscribers a broker is managing (State).
     */
    public final IntSummaryStatistics inputTableStats = new IntSummaryStatistics();
    
    /**
     * Statistics (Min, Max, Avg, Count) for the size of the "Output Table" (Subscriptions sent).
     * Represents the number of parents/neighbors a broker is subscribed to (State).
     */
    public final IntSummaryStatistics outputTableStats = new IntSummaryStatistics();
    
    // --- Common: Traffic & Processing Load ---
    
    /**
     * Total number of subscription updates received by brokers from their children.
     * This measures the raw "Processing Load" regarding topology maintenance.
     * Unlike table size (state), this counts the volume of incoming change requests (events).
     */
    public long totalSubscriptionInputEvents = 0;

    /**
     * Total number of publications successfully injected into the network by Publishers.
     * This counts only messages successfully handed off to a Gateway Broker. 
     * It does not include publications that were created but failed to send or found no gateway.
     */
    public long totalPublicationsSent = 0;

    /**
     * Total number of times a publication was processed by a broker (Fan-In).
     * Strictly measures "Broker Work". 
     * CRITICAL: This excludes the final delivery processing at the Subscriber (Last Mile).
     */
    public long totalPublicationProcessingEvents = 0;

    /**
     * Total number of geometric intersection checks (Region) or distance calculations (Proximity) performed.
     * This serves as a hardware-agnostic proxy for CPU cost.
     */
    public long totalMatchingComputations = 0; 

    // --- Common: Delivery & Accuracy ---

    /**
     * Total number of publications successfully delivered to subscribers.
     * This is the primary measure of "Service Value".
     * It also acts as the denominator for calculating Average Hop Count.
     */
    public long totalDeliveriesReceived = 0;

    /**
     * Count of "Dead End" forwarding events where a broker found a potential match in its index
     * but subsequently found no valid children to forward to.
     */
    public long totalFalsePositiveEvents = 0;

    /**
     * Count of messages delivered to a subscriber that did not actually match their interest.
     * Common in Regional simulations due to aggregation "corners" (MBR) covering empty space.
     */
    public long totalFalsePositiveDeliveries = 0;
    
    // --- Common: Hops (Network Latency Proxy) ---

    /**
     * The cumulative sum of graph edges (hops) traversed by every single delivered message.
     * Used as the numerator to calculate Average Hop Count: (totalHopSum / totalDeliveriesReceived).
     */
    public long totalHopSum = 0;

    /**
     * The shortest path length observed for any delivered message.
     * Typically 2 (Publisher -> Gateway -> Subscriber) or similar small integer.
     */
    public int globalMinHops = Integer.MAX_VALUE;

    /**
     * The longest path length observed for any delivered message.
     * High values indicate inefficient routing paths or unbalanced tree topologies.
     */
    public int globalMaxHops = Integer.MIN_VALUE;
    
    // --- Ground Truth ---
    /**
     * The number of matches that SHOULD have occurred according to an omniscient Oracle.
     * Compared against totalDeliveriesReceived to calculate Accuracy (Recall).
     */
    public long groundTruthMatches = 0; 
    
    /**
     * Returns a human-readable label for the algorithm (e.g., "Regional (Heavy)", "Proximity (Light)").
     * Used in the report banner.
     */    
    public abstract String getAlgorithmLabel();
}
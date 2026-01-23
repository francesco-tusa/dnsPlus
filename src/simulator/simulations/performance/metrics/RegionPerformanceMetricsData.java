package simulator.simulations.performance.metrics;

public class RegionPerformanceMetricsData extends PerformanceMetricsData {

    // --- Input Metrics (Processing Logic) ---
    // Measures how the broker handled subscription requests arriving from any neighbor (Children or Parent).

    public long totalInputCovered = 0; // Updates fully contained in existing region (Suppressed locally)
    public long totalInputExpanded = 0; // Total expansions triggered (Simple + Complex)
    
    public long totalInputSimpleExpanded = 0; // Stable growth (1-to-1 table update)
    public long totalInputComplexExpanded = 0; // Optimization growth (Merged disjoint regions)
    public long totalInputAdded = 0; // New disjoint entries added to the routing table

    // --- Optimization Artifacts (Side-effects) ---
    // Entries removed from the routing table during complex expansions.
    
    public long totalInputMerged = 0; // Entries removed via merge
    public long totalInputAbsorbed = 0; // Entries removed via absorption (fully covered by neighbor)

    // --- Propagation Metrics (Output Logic) ---
    // Measures the aggregated updates sent (or suppressed) to ANY neighbor (Parent OR Children).
    // Note: The Regional algorithm propagates subscription state bidirectionally.

    public long totalPropagatedExpanded = 0; // Expansion updates sent to a neighbor (Upstream or Downstream)
    public long totalPropagatedSimpleExpanded = 0;
    public long totalPropagatedComplexExpanded = 0;
    
    public long totalPropagatedAdded = 0; // New entry updates sent to a neighbor
    
    // "Second-level" aggregation: We intended to send, but the neighbor's existing subscription already covered it.
    public long totalPropagatedCovered = 0; 
    
    public long totalPropagatedMerged = 0; // Side-effect of propagation merge (entry removed from output)
    public long totalPropagatedAbsorbed = 0; // Side-effect of propagation absorption (entry removed from output)

    @Override
    public String getAlgorithmLabel() {
        return "Regional (Heavy / Spatial Match)";
    }
}
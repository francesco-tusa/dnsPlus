package simulator.simulations.performance.metrics;

public class RegionPerformanceMetricsData extends PerformanceMetricsData {

    // Unique to Region: Geometric Operations (Input)
    public long totalSubCovered = 0;
    public long totalSubExpanded = 0;
    public long totalSubAdded = 0;
    public long totalSubAbsorbed = 0;
    public long totalSubMerged = 0;

    // Unique to Region: Geometric Operations (Output)
    public long totalOutSubCovered = 0;
    public long totalOutSubExpanded = 0;
    public long totalOutSubAdded = 0;
    public long totalOutSubAbsorbed = 0;
    public long totalOutSubMerged = 0;

    @Override
    public String getAlgorithmLabel() {
        return "REGION (Spatial Match)";
    }
}
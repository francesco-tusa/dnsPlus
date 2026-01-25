package simulator.experimentation;

import simulator.simulations.performance.PopulationGeonamesRegionPerformanceSimulation;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import java.util.Properties;

public class RegionalScenario implements SimulationScenario {

    @Override
    public String getKnobKey() {
        return "broker.smartThreshold";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "0.0", "0.1", "0.25", "0.5", "1.0" };
    }

    @Override
    public String getCsvHeader() {
        // Columns:
        // 1. publishers
        // 2. subscribers
        // 3. fpr_threshold
        // 4. table_size_avg
        // 5. pub_events (Total Forwarding Events)
        // 6. avg_comparisons (Computations per Event)
        // 7. sub_overhead (Expanded + Added)
        // 8. traffic_ratio (Events per Delivery)
        // 9. traffic_saved (Covered/Filtered)
        return "publishers,subscribers,fpr_threshold,table_size_avg,pub_events,avg_comparisons,sub_updates_sent,traffic_ratio,traffic_saved";
    }

    @Override
    public void configure(Properties props) {
        props.setProperty("broker.brake.strategy", "simulator.regions.policy.NoOpBrakeStrategy");
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        var sim = new PopulationGeonamesRegionPerformanceSimulation();
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new SpatialMatchBrokerFactory());
        sim.run(factory, config);
        return sim.getLastRunMetrics();
    }

    @Override
public String getCsvRow(PerformanceMetricsData data, Properties props) {
    // Type Check
    if (!(data instanceof RegionPerformanceMetricsData regionData)) {
        return "";
    }

    // --- 1. Retrieve Config Props ---
    String publishers = props.getProperty("workload.replicas", "Unknown");
    String subscribers = props.getProperty("workload.subscribers.count", "Unknown");
    String knobValue = props.getProperty(getKnobKey(), "0.0");

    // --- 2. Retrieve Table Stats ---
    double avgTableSize = regionData.inputTableStats.getAverage();

    // --- 3. Calculate Pub Overhead & Comparisons ---
    long pubEvents = regionData.totalPublicationProcessingEvents;
    long computations = regionData.totalMatchingComputations;
    
    // Avoid division by zero
    double avgComparisons = (pubEvents > 0) 
            ? (double) computations / pubEvents 
            : 0.0;

    // --- 4. Calculate Sub Overhead ---
    long subOverhead = regionData.totalPropagatedExpanded + regionData.totalPropagatedAdded;

    // --- 5. Calculate Traffic Ratio ---
    double trafficRatio = (regionData.totalDeliveriesReceived > 0) 
            ? (double) regionData.totalPublicationProcessingEvents / regionData.totalDeliveriesReceived 
            : 0.0;

    // --- 6. Retrieve Traffic Saved ---
    long trafficSaved = regionData.totalPropagatedCovered;

    // Format output
    return String.format("%s,%s,%s,%.2f,%d,%.2f,%d,%.4f,%d",
            publishers,
            subscribers,
            knobValue,
            avgTableSize,
            pubEvents,
            avgComparisons,
            subOverhead,
            trafficRatio,
            trafficSaved
    );
}
}
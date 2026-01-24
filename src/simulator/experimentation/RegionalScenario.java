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
    public String getKnobKey() { return "broker.smartThreshold"; }

    @Override
    public String[] getKnobValues() { return new String[]{"0.0", "0.1", "0.25", "0.5", "1.0"}; }

    @Override
    public String getCsvHeader() { 
        return "publishers,subscribers,fpr_threshold,table_size_avg,pub_overhead,sub_overhead,traffic_ratio,covered_subs"; 
    }

    // --- NEW METHOD ---
    @Override
    public void configure(Properties props) {
        // Explicitly disable the brake (good practice to ensure no side effects)
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
    public String getCsvRow(PerformanceMetricsData metrics, Properties config) {
        RegionPerformanceMetricsData regMetrics = (RegionPerformanceMetricsData) metrics;
        return String.format("%s,%s,%s,%.2f,%d,%d,%.4f,%d",
            config.getProperty("workload.publishers.count"),
            config.getProperty("workload.subscribers.count"),
            config.getProperty(getKnobKey()), 
            metrics.inputTableStats.getAverage(),
            metrics.totalPublicationsSent,
            metrics.totalSubscriptionInputEvents, 
            (double) metrics.totalPublicationProcessingEvents / Math.max(1, metrics.totalDeliveriesReceived),
            regMetrics.totalInputCovered
        );
    }
}
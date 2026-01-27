package simulator.experimentation;

import simulator.simulations.performance.AwsGeonamesLocationPerformanceSimulation;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import java.util.Properties;

public class ProximityScenario implements SimulationScenario {

    @Override
    public String getKnobKey() { return "proximity.brake.limit"; }

    @Override
    public String[] getKnobValues() { return new String[]{"1", "5", "10", "1000"}; }

    @Override
    public String getCsvHeader() { 
        return "publishers,subscribers,brake_limit,table_size_avg,pub_overhead,sub_overhead,traffic_ratio,accuracy,stretch"; 
    }

    // --- NEW METHOD ---
    @Override
    public void configure(Properties props) {
        // This is the logic moved from the Orchestrator
        props.setProperty("broker.brake.strategy", "simulator.regions.policy.DecayingCounterBrakeStrategy");
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        var sim = new AwsGeonamesLocationPerformanceSimulation();
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new LocationBrokerFactory());
        sim.run(factory, config);
        return sim.getLastRunMetrics();
    }

    @Override
    public String getCsvRow(PerformanceMetricsData metrics, Properties config) {
        double stretch = (metrics.totalDeliveriesReceived > 0) 
            ? (double) metrics.totalHopSum / metrics.totalDeliveriesReceived : 0.0;
            
        return String.format("%s,%s,%s,%.2f,%d,%d,%.4f,%.4f,%.2f",
            config.getProperty("workload.publishers.count"),
            config.getProperty("workload.subscribers.count"),
            config.getProperty(getKnobKey()), 
            metrics.inputTableStats.getAverage(),
            metrics.totalPublicationsSent,
            metrics.totalSubscriptionInputEvents,
            (double) metrics.totalPublicationProcessingEvents / Math.max(1, metrics.totalDeliveriesReceived),
            (double) metrics.totalDeliveriesReceived / Math.max(1, metrics.groundTruthMatches),
            stretch
        );
    }
}
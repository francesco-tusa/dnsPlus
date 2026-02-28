package marketplace.simulations;

import java.util.Properties;
import simulator.experimentation.AbstractSimulationScenario;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import marketplace.topology.MarketplaceBrokerFactory;

public class MarketplaceScenario extends AbstractSimulationScenario {

    @Override
    public String getKnobKey() {
        return "broker.smartThreshold";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "0.10", "0.25", "0.50", "0.75", "1.00" };
    }

    @Override
    public String getCsvHeader() {
        // CLEAN METRICS SEPARATION
        // Control Plane (State): state_added, state_expanded, state_suppressed
        // Data Plane (Requests): processed_requests_load, qos_evaluations, shielded_requests, dead_ends
        return getCommonCsvHeader() + ",threshold,cloud_nodes,fog_nodes,edge_nodes,rep_id," +
               "table_size_avg,state_added,state_expanded,state_suppressed," +
               "processed_requests_load,qos_evaluations,shielded_requests,dead_ends," +
               "optimal_matches,accuracy";
    }

    @Override
    protected void configureSpecific(Properties props) {
        props.setProperty("broker.brake.strategy", "simulator.regions.policy.NoOpBrakeStrategy");
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        var sim = new SystematicMarketplaceContinuumSimulation();
        MarketplaceTopologyConfiguration config = new MarketplaceTopologyConfiguration();
        MarketplaceTopologyLoader factory = new MarketplaceTopologyLoader(config, new MarketplaceBrokerFactory());
        
        sim.run(factory, config);
        this.currentExperimentId = sim.getSimulationId();
        return sim.getLastRunMetrics();
    }

    @Override
    public String getCsvRow(PerformanceMetricsData data, Properties props) {
        if (!(data instanceof RegionPerformanceMetricsData regionData)) {
            return "";
        }

        double avgTableSize = (regionData.inputTableStats.getCount() > 0) 
            ? regionData.inputTableStats.getAverage() : 0.0;

        // --- DECOUPLED CONTROL PLANE METRICS (State) ---
        long stateAdded = regionData.totalPropagatedAdded;
        long stateExpanded = regionData.totalPropagatedExpanded;
        long stateSuppressed = regionData.totalInputCovered;

        // --- DECOUPLED DATA PLANE METRICS (Requests) ---
        long pubEvents = regionData.totalPublicationProcessingEvents;
        long totalQosEvaluations = regionData.totalMatchingComputations; 
        long shieldedRequests = regionData.totalProactiveShieldedEvents;
        long deadEnds = regionData.totalDownwardDeadEndEvents;
        
        long optimalMatches = regionData.groundTruthMatches; 
        long deliveries = regionData.totalDeliveriesReceived;
        
        double accuracy = safeDiv(deliveries, (double) optimalMatches);

        return String.format("%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%s",
                getCommonCsvPrefix(props),
                props.getProperty(getKnobKey(), "0.0"),
                props.getProperty("marketplace.providers.cloud.count", "0"),
                props.getProperty("marketplace.providers.fog.count", "0"),
                props.getProperty("marketplace.providers.edge.count", "0"),
                props.getProperty("marketplace.repetition.id", "1"),
                formatDouble(avgTableSize),
                stateAdded,                     // NEW
                stateExpanded,                  // NEW
                stateSuppressed,                // NEW
                pubEvents,
                totalQosEvaluations,
                shieldedRequests,               // NEW
                deadEnds,
                optimalMatches,
                formatDouble(accuracy)
        );
    }
}
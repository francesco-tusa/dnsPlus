package marketplace.simulations;

import java.util.Properties;
import simulator.experimentation.AbstractSimulationScenario;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import marketplace.analysis.MarketplacePerformanceMetricsData;
import marketplace.topology.MarketplaceBrokerFactory;

public class MarketplaceScenario extends AbstractSimulationScenario {

    @Override
    public String getKnobKey() {
        return "broker.smartThreshold";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "0.0", "0.05", "0.10", "0.15", "0.20", "0.25", "0.30", "0.40", "0.50", "0.60", "0.75", "1.00" };
    }

    @Override
    public String getCsvHeader() {
        return getCommonCsvHeader() + ",threshold,cloud_nodes,fog_nodes,edge_nodes,rep_id," +
               "table_size_avg,state_added,state_expanded,state_suppressed," +
               "processed_requests_load,qos_evaluations,shielded_requests,dead_ends," +
               "ground_truth_matches,deliveries,accuracy," + 
               "true_optimal_deliveries,suboptimal_deliveries,avg_optimality_gap";
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
        // Upgrade the cast to our new Marketplace container
        if (!(data instanceof MarketplacePerformanceMetricsData marketData)) {
            return "";
        }

        double avgTableSize = (marketData.inputTableStats.getCount() > 0) 
            ? marketData.inputTableStats.getAverage() : 0.0;

        // --- DECOUPLED CONTROL PLANE METRICS (State) ---
        long stateAdded = marketData.totalPropagatedAdded;
        long stateExpanded = marketData.totalPropagatedExpanded;
        long stateSuppressed = marketData.totalPropagatedCovered;

        // --- DECOUPLED DATA PLANE METRICS (Requests) ---
        long pubEvents = marketData.totalPublicationProcessingEvents;
        long totalQosEvaluations = marketData.totalMatchingComputations; 
        long shieldedRequests = marketData.totalProactiveShieldedEvents;
        long deadEnds = marketData.totalDownwardDeadEndEvents;
        
        long groundTruthMatches = marketData.groundTruthMatches; 
        long deliveries = marketData.totalDeliveriesReceived;
        
        double accuracy = safeDiv(deliveries, (double) groundTruthMatches);

        // --- NEW MULTI-OBJECTIVE METRICS (Deferred Utility Synthesis) ---
        long trueOptimalDeliveries = marketData.optimalDeliveries;
        long suboptimalDeliveries = marketData.suboptimalDeliveries;
        double avgOptimalityGap = marketData.getAverageUtilityDegradation();

        // Ensure formatting sequence matches the new header exactly (20 elements)
        return String.format("%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%d,%d,%s",
                getCommonCsvPrefix(props),
                props.getProperty(getKnobKey(), "0.0"),
                props.getProperty("marketplace.providers.cloud.count", "0"),
                props.getProperty("marketplace.providers.fog.count", "0"),
                props.getProperty("marketplace.providers.edge.count", "0"),
                props.getProperty("marketplace.repetition.id", "1"),
                formatDouble(avgTableSize),
                stateAdded,
                stateExpanded,
                stateSuppressed,
                pubEvents,
                totalQosEvaluations,
                shieldedRequests,
                deadEnds,
                groundTruthMatches,
                deliveries,
                formatDouble(accuracy),
                trueOptimalDeliveries,
                suboptimalDeliveries,
                formatDouble(avgOptimalityGap)
        );
    }
}
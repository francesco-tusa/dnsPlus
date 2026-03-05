package marketplace.simulations;

import java.util.Properties;
import simulator.experimentation.AbstractSimulationScenario;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import marketplace.analysis.MarketplacePerformanceMetricsData;
import marketplace.topology.MarketplaceBrokerFactory;

public abstract class AbstractMarketplaceScenario extends AbstractSimulationScenario {

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
    public String getCsvHeader() {
        return getCommonCsvHeader() + ",strategy,sweep_value,cloud_nodes,fog_nodes,edge_nodes,rep_id," +
               "table_size_avg,state_added,state_expanded,state_suppressed," +
               "processed_requests_load,qos_evaluations,shielded_requests,dead_ends," +
               "ground_truth_matches,deliveries,accuracy," + 
               "true_optimal_deliveries,suboptimal_deliveries,sla_violations,avg_optimality_gap," + 
               "avg_delivered_utility";
    }

    @Override
    public String getCsvRow(PerformanceMetricsData data, Properties props) {
        if (!(data instanceof MarketplacePerformanceMetricsData marketData)) {
            return "";
        }

        double avgTableSize = (marketData.inputTableStats.getCount() > 0) 
            ? marketData.inputTableStats.getAverage() : 0.0;

        long stateAdded = marketData.totalPropagatedAdded;
        long stateExpanded = marketData.totalPropagatedExpanded;
        long stateSuppressed = marketData.totalPropagatedCovered;

        long pubEvents = marketData.totalPublicationProcessingEvents;
        long totalQosEvaluations = marketData.totalMatchingComputations; 
        long shieldedRequests = marketData.totalProactiveShieldedEvents;
        long deadEnds = marketData.totalDownwardDeadEndEvents;
        
        long groundTruthMatches = marketData.groundTruthMatches; 
        long deliveries = marketData.totalDeliveriesReceived;
        
        double accuracy = safeDiv(deliveries, (double) groundTruthMatches);

        long trueOptimalDeliveries = marketData.optimalDeliveries;
        long suboptimalDeliveries = marketData.suboptimalDeliveries;
        long slaViolations = marketData.slaViolations;
        double avgOptimalityGap = marketData.getAverageUtilityDegradation();
        double avgDeliveredUtility = marketData.getAverageDeliveredUtility();

        String strategy = props.getProperty("marketplace.routing.strategy", "WEIGHTED_UTILITY");

        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%d,%d,%d,%s,%s",
                getCommonCsvPrefix(props),
                strategy,
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
                slaViolations,
                formatDouble(avgOptimalityGap),
                formatDouble(avgDeliveredUtility)
        );
    }
}
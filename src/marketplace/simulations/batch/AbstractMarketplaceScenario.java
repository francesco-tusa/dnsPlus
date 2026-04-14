package marketplace.simulations.batch;

import java.util.Properties;
import java.util.Map;
import java.util.IntSummaryStatistics;
import java.util.function.Supplier;

import simulator.experimentation.AbstractSimulationScenario;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import marketplace.analysis.MarketplacePerformanceMetricsData;
import marketplace.simulations.MarketplaceTopologyConfiguration;
import marketplace.simulations.MarketplaceTopologyLoader;
import marketplace.simulations.AbstractMarketplaceContinuumSimulation;
import marketplace.topology.MarketplaceBrokerFactory;
import marketplace.config.MarketplaceConfig;
import marketplace.workload.traces.AzureTraceRepository;

public abstract class AbstractMarketplaceScenario extends AbstractSimulationScenario {

    protected final Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory;

    public AbstractMarketplaceScenario(Supplier<AbstractMarketplaceContinuumSimulation> simulationFactory) {
        this.simulationFactory = simulationFactory;
    }
    
    @Override
    protected void configureSpecific(Properties props) {
        props.setProperty("broker.brake.strategy", "simulator.regions.policy.NoOpBrakeStrategy");
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        // 1. Force the trace repository to explicitly parse the CSV using the 
        // dynamically swept configuration limits BEFORE the workload engine boots.
        String traceFile = MarketplaceConfig.get().azureTraceFilePath;
        AzureTraceRepository.getInstance().loadTraces(traceFile);
        
        // 2. Fetch a fresh polymorphic instance (Azure or Legacy)
        AbstractMarketplaceContinuumSimulation sim = simulationFactory.get();
        
        // 3. Setup standard topology constraints
        MarketplaceTopologyConfiguration config = new MarketplaceTopologyConfiguration();
        MarketplaceTopologyLoader factory = new MarketplaceTopologyLoader(config, new MarketplaceBrokerFactory());
        
        // 4. Execute the simulation
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
               "avg_delivered_utility," +
               "feasible_sla_violations,unfeasible_sla_violations,avg_sla_gap," +
               "unified_system_degradation," +
               "he_linear_total,he_bst_total,he_linear_per_req,he_bst_per_req,qos_evals_per_req," +
               "cloud_dir_size,fog_dir_size,edge_dir_size," +
               "cloud_spatial_size,fog_spatial_size,edge_spatial_size";
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
        
        // --- 1. OVERALL HE METRIC NORMALIZATION ---
        long heLinear = marketData.totalIdLinearComputations;
        long heBst = marketData.totalIdBstComputations;
        
        // Calculate the theoretical total requests injected to normalize the crypto bounds
        double replicas = Double.parseDouble(props.getProperty("workload.replicas", "1.0"));
        double meanSubs = Double.parseDouble(props.getProperty("workload.meanSubscriptionsPerSubscriber", "1.0"));
        double injectedReqs = replicas * meanSubs;
        if (injectedReqs <= 0) injectedReqs = 1.0; 
        
        double linearPerReq = heLinear / injectedReqs;
        double bstPerReq = heBst / injectedReqs;
        double qosEvalsPerReq = totalQosEvaluations / injectedReqs;

        // --- 2. TIER-SPECIFIC STATE EXTRACTION (For Python Post-Processing) ---
        // Level 0 = Cloud, Level 1 = Fog, Level 2 = Edge
        double cloudDirSize = getTierAverage(marketData.tierFunctionDirectoryStats, 0);
        double fogDirSize   = getTierAverage(marketData.tierFunctionDirectoryStats, 1);
        double edgeDirSize  = getTierAverage(marketData.tierFunctionDirectoryStats, 2);

        double cloudSpatialSize = getTierAverage(marketData.tierSpatialIndexStats, 0);
        double fogSpatialSize   = getTierAverage(marketData.tierSpatialIndexStats, 1);
        double edgeSpatialSize  = getTierAverage(marketData.tierSpatialIndexStats, 2);

        String strategy = props.getProperty("marketplace.routing.strategy", "WEIGHTED_UTILITY");

        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%d,%d,%d,%s,%s,%d,%d,%s,%s,%d,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                getCommonCsvPrefix(props), strategy, props.getProperty(getKnobKey(), "0.0"),
                props.getProperty("marketplace.providers.cloud.count", "0"),
                props.getProperty("marketplace.providers.fog.count", "0"),
                props.getProperty("marketplace.providers.edge.count", "0"),
                props.getProperty("marketplace.repetition.id", "1"),
                formatDouble(avgTableSize), stateAdded, stateExpanded, stateSuppressed, pubEvents, totalQosEvaluations,
                shieldedRequests, deadEnds, groundTruthMatches, deliveries, formatDouble(accuracy),
                trueOptimalDeliveries, suboptimalDeliveries, slaViolations,
                formatDouble(marketData.getAverageUtilityDegradation()), formatDouble(marketData.getAverageDeliveredUtility()),
                marketData.feasibleSlaViolations, marketData.unfeasibleSlaViolations,
                formatDouble(marketData.getAverageSlaViolationDegradation()), formatDouble(marketData.getUnifiedSystemDegradation()),
                heLinear, heBst, formatDouble(linearPerReq), formatDouble(bstPerReq), formatDouble(qosEvalsPerReq),
                formatDouble(cloudDirSize), formatDouble(fogDirSize), formatDouble(edgeDirSize),
                formatDouble(cloudSpatialSize), formatDouble(fogSpatialSize), formatDouble(edgeSpatialSize)
        );
    }

    /**
     * Helper method to safely extract the statistical average from the tier maps.
     */
    private double getTierAverage(Map<Integer, IntSummaryStatistics> statsMap, int tierLevel) {
        if (statsMap != null && statsMap.containsKey(tierLevel)) {
            IntSummaryStatistics stats = statsMap.get(tierLevel);
            if (stats.getCount() > 0) {
                return stats.getAverage();
            }
        }
        return 0.0;
    }
}
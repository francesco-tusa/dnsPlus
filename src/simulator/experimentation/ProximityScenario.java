package simulator.experimentation;

import simulator.simulations.performance.AwsGeonamesLocationPerformanceSimulation;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.ProximityPerformanceMetricsData;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import java.util.Properties;

public class ProximityScenario extends AbstractSimulationScenario {

    @Override
    public String getKnobKey() {
        return "proximity.brake.limit";
    }

    @Override
    public String[] getKnobValues() {
        return new String[] { "-1", "4", "2", "1" };
    }

    @Override
    public String getCsvHeader() {
        return getCommonCsvHeader() + ",brake_limit,table_size_avg,core_table_size_avg," + 
               "sub_input_events,sub_output_events,sub_agg_factor," + 
               "pubs_sent,pub_processed_events,pub_brake_suppressed,pub_forwarded_events," + 
               "dead_ends,matching_cost,traffic_ratio," + 
               "ground_truth,deliveries,recall," + 
               "min_stretch,max_stretch,avg_stretch";
    }

    @Override
    protected void configureSpecific(Properties props) {
        String limit = props.getProperty(getKnobKey());
        if (limit != null && !limit.equals("-1")) {
            props.setProperty("proximity.brake.enabled", "true");
        } else {
            props.setProperty("proximity.brake.enabled", "false");
        }
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        var sim = new AwsGeonamesLocationPerformanceSimulation();
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new LocationBrokerFactory());
        
        sim.run(factory, config);
        
        this.currentExperimentId = sim.getSimulationId();
        
        return sim.getLastRunMetrics();
    }

    @Override
    public String getCsvRow(PerformanceMetricsData metrics, Properties config) {
        // 1. Cast to Proximity Data to access specific fields
        ProximityPerformanceMetricsData pd = (metrics instanceof ProximityPerformanceMetricsData) 
            ? (ProximityPerformanceMetricsData) metrics 
            : null;

        if (pd == null) {
            return "ERROR: Invalid Metrics Data Type";
        }

        // 2. Calculate Derived Metrics
        double avgCoreTable = (metrics.coreInputTableStats.getCount() > 0) 
            ? metrics.coreInputTableStats.getAverage() : 0.0;

        // Aggregation Factor: Input / Output
        double aggFactor = safeDiv(pd.totalSubscriptionInputEvents, pd.totalUpstreamSubscriptionUpdates);
        if (pd.totalSubscriptionInputEvents > 0 && pd.totalUpstreamSubscriptionUpdates == 0) {
            // Infinite aggregation (absorbed all traffic)
            aggFactor = Double.POSITIVE_INFINITY; 
        }

        double recall = safeDiv(pd.totalDeliveriesReceived, pd.groundTruthMatches);
        
        // Traffic Ratio: Processed / Deliveries
        double trafficRatio = safeDiv(pd.totalPublicationProcessingEvents, pd.totalDeliveriesReceived);

        // Stretch Stats (Converted to KM for consistency with Logs)
        // Degrees * 111.1 = Approx KM
        double minStretchKm = (pd.stretchStats.getCount() > 0) ? pd.stretchStats.getMin() * 111.1 : 0.0;
        double maxStretchKm = (pd.stretchStats.getCount() > 0) ? pd.stretchStats.getMax() * 111.1 : 0.0;
        double avgStretchKm = (pd.stretchStats.getCount() > 0) ? pd.stretchStats.getAverage() * 111.1 : 0.0;

        // 3. Construct CSV String
        // Usage of formatDouble ensures consistency (4 decimal places) as requested.
        return String.format("%s,%s,%s,%s,%d,%d,%s,%d,%d,%d,%d,%d,%d,%s,%d,%d,%s,%s,%s,%s",
                getCommonCsvPrefix(config),              // experiment_id, pubs, subs
                config.getProperty(getKnobKey()),        // brake_limit
                
                // STATE
                formatDouble(metrics.inputTableStats.getAverage()), // table_size_avg
                formatDouble(avgCoreTable),                         // core_table_size_avg
                
                // SUBSCRIPTION TRAFFIC
                pd.totalSubscriptionInputEvents,         // sub_input_events (Long)
                pd.totalUpstreamSubscriptionUpdates,     // sub_output_events (Long)
                (Double.isInfinite(aggFactor)) ? "Infinite" : formatDouble(aggFactor), // sub_agg_factor
                
                // PUBLICATION TRAFFIC
                pd.totalPublicationsSent,                // pubs_sent
                pd.totalPublicationProcessingEvents,     // pub_processed_events
                pd.totalBrakeSuppressedEvents,           // pub_brake_suppressed
                pd.totalPublicationsForwarded,           // pub_forwarded_events 
                
                // ROUTING EFFICIENCY
                pd.totalFalsePositiveEvents,             // dead_ends
                pd.totalMatchingComputations,            // matching_cost
                formatDouble(trafficRatio),              // traffic_ratio
                
                // ACCURACY
                pd.groundTruthMatches,                   // ground_truth
                pd.totalDeliveriesReceived,              // deliveries
                formatDouble(recall),                    // recall
                
                // STRETCH (KM)
                formatDouble(minStretchKm),
                formatDouble(maxStretchKm),
                formatDouble(avgStretchKm)
        );
    }
}
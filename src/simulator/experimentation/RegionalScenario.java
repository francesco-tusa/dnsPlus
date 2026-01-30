package simulator.experimentation;

import simulator.simulations.performance.PopulationGeonamesRegionPerformanceSimulation;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import java.util.Properties;

public class RegionalScenario extends AbstractSimulationScenario {

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
        return getCommonCsvHeader() + ",fpr_threshold," +
               "table_size_avg,core_table_size_avg," +
               "pub_events,avg_comparisons," +
               "sub_updates_sent," +
               "traffic_ratio,traffic_saved,suppression_rate," +
               "false_positives,false_positive_rate";
    }

    @Override
    protected void configureSpecific(Properties props) {
        props.setProperty("broker.brake.strategy", "simulator.regions.policy.NoOpBrakeStrategy");
    }

    @Override
    public PerformanceMetricsData runSimulation() {
        var sim = new PopulationGeonamesRegionPerformanceSimulation();
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new SpatialMatchBrokerFactory());
        
        sim.run(factory, config);
        
        this.currentExperimentId = sim.getSimulationId();

        return sim.getLastRunMetrics();
    }

    @Override
    public String getCsvRow(PerformanceMetricsData data, Properties props) {
        if (!(data instanceof RegionPerformanceMetricsData regionData)) {
            return "";
        }

        // Stats calculation
        double avgTableSize = regionData.inputTableStats.getAverage();
        double avgCoreTableSize = (regionData.coreInputTableStats.getCount() > 0) 
            ? regionData.coreInputTableStats.getAverage() : 0.0;
        long pubEvents = regionData.totalPublicationProcessingEvents;
        long computations = regionData.totalMatchingComputations;
        double avgComparisons = (pubEvents > 0) ? (double) computations / pubEvents : 0.0;
        long subOverhead = regionData.totalPropagatedExpanded + regionData.totalPropagatedAdded;
        double trafficRatio = (regionData.totalDeliveriesReceived > 0) 
                ? (double) regionData.totalPublicationProcessingEvents / regionData.totalDeliveriesReceived : 0.0;
        long trafficSaved = regionData.totalInputCovered + regionData.totalPropagatedCovered;
        long totalInput = regionData.totalInputCovered + regionData.totalInputExpanded + regionData.totalInputAdded;
        double suppressionRate = (totalInput > 0) ? (double) trafficSaved / totalInput : 0.0;
        long falsePositives = regionData.totalFalsePositiveEvents;
        double fpRate = (pubEvents > 0) ? (double) falsePositives / pubEvents * 100.0 : 0.0;

        return String.format("%s,%s,%.2f,%.2f,%d,%.2f,%d,%.4f,%d,%.4f,%d,%.2f",
                getCommonCsvPrefix(props),
                props.getProperty(getKnobKey(), "0.0"),
                avgTableSize,
                avgCoreTableSize,
                pubEvents,
                avgComparisons,
                subOverhead,
                trafficRatio,
                trafficSaved,
                suppressionRate,
                falsePositives,
                fpRate
        );
    }
}
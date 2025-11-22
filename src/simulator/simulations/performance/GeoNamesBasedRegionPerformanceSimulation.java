package simulator.simulations.performance;

import simulator.topology.TopologyPaths;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * Abstract base class for Region-Based simulations using GeoNames.
 * Centralizes the topology loading logic using the default Full Topology.
 */
public abstract class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability,
                                                    boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, enableCsvOutput);
    }

    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability) {
        this(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, false);
    }
    
    /**
     * Runs the simulation using the default FULL_TOPOLOGY configured in TopologyPaths.
     */
    public void run() {
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        super.run(factory, config);
    }
}
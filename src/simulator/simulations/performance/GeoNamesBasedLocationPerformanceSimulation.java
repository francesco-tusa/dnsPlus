package simulator.simulations.performance;

import simulator.topology.TopologyPaths;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * Abstract base class for Location-Based simulations using GeoNames.
 */
public abstract class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                      boolean enableCsvOutput) { 
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); 
    }
    
    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false);
    }
    
    /**
     * Runs the simulation using the default FULL_TOPOLOGY configured in TopologyPaths.
     */
    public void run() {
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        super.run(factory, config);
    }
}
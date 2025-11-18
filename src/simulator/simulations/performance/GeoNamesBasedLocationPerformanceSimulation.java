package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import utils.CustomLogger; // Import CustomLogger

/**
 * This is now an ABSTRACT base class for simulations that use the GeoNames
 * topology and LOCATION-based routing.
 * It cannot be run directly. Run its subclasses instead.
 */
public abstract class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    private static final Logger logger = CustomLogger.getLogger(GeoNamesBasedLocationPerformanceSimulation.class.getName()); // Get logger

    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                      boolean enableCsvOutput) { 
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); 
    }
    
    // Legacy constructor
    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false);
    }

    // --- MAIN METHOD REMOVED ---
    // This class is now abstract. Run AwsGeonamesLocationPerformanceSimulation
    // or PopulationGeonamesLocationPerformanceSimulation instead.
}
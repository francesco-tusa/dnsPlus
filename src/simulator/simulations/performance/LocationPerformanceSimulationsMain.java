package simulator.simulations.performance;

/**
 * Main entry point for all PERFORMANCE simulations using LOCATION-BASED routing brokers.
 */
public class LocationPerformanceSimulationsMain {

    public static void main(String[] args) {
        // Run a smaller-scale performance test on a random topology
        RandomTopologyLocationPerformanceSimulation.main(args);
        
        // Uncomment the line below to run the full-scale, realistic performance test
        // GeoNamesBasedLocationPerformanceSimulation.main(args);
    }
}
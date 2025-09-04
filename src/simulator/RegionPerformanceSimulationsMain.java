package simulator;

import simulator.simulations.performance.GeoNamesBasedRegionPerformanceSimulation;
import simulator.simulations.performance.RandomTopologyRegionPerformanceSimulation;

/**
 * Main entry point for all PERFORMANCE simulations using REGION-BASED routing brokers.
 */
public class RegionPerformanceSimulationsMain {

    public static void main(String[] args) {
        // Run a smaller-scale performance test on a random topology
        //RandomTopologyRegionPerformanceSimulation.main(args);
        
        // Uncomment the line below to run the full-scale, realistic performance test
        GeoNamesBasedRegionPerformanceSimulation.main(args);
    }
}
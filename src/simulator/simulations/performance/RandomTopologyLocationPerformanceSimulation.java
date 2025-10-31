package simulator.simulations.performance;

import java.util.logging.Level; // Import Level
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

public class RandomTopologyLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    public RandomTopologyLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        super(numberOfReplicas, subscribersPerReplica);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Random Topology Performance Simulation (Location-Based) ---");
        
        int simNumberOfReplicas = 10;
        int simSubscribersPerReplica = 100;
        
        // --- New logging flag ---
        boolean enableVerboseLogs = true; // Set to true to see debug placement logs

        int topologyTreeDepth = 4;
        int topologyMaxBranching = 3;
        int topologyNumRegions = 5;

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(
            topologyTreeDepth, topologyMaxBranching, topologyNumRegions, 0, 0);
            
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new LocationBrokerFactory());
        
        RandomTopologyLocationPerformanceSimulation simulation = new RandomTopologyLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica
        );
        
        // --- Use the new setter to enable verbose logs ---
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            System.out.println("--- VERBOSE LOGGING ENABLED (Level.FINE) ---");
        }
        
        simulation.run(factory, config);
    }
}
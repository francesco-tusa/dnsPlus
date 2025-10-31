package simulator.simulations.performance;

import java.util.logging.Level; // Import Level
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

public class RandomTopologyRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    public RandomTopologyRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                     double subscriptionRegionSize, double remoteInterestProbability) { 
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability); 
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Random Topology Performance Simulation (Region-Based) ---");

        // --- EASILY ADJUSTABLE PARAMETERS ---
        int simNumberOfReplicas = 10;
        int simSubscribersPerReplica = 100;
        double simSubscriptionRegionSize = 10.0;
        double simRemoteInterestProbability = 0.2;
        
        // --- New logging flag ---
        boolean enableVerboseLogs = false; // Set to true to see debug placement logs

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(4, 3, 5, 0, 0);
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new RegionBrokerFactory());

        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, simSubscriptionRegionSize, simRemoteInterestProbability
        );

        // --- Use the new setter to enable verbose logs ---
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            System.out.println("--- VERBOSE LOGGING ENABLED (Level.FINE) ---");
        }

        simulation.run(factory, config);
    }
}
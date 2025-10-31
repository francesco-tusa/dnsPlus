package simulator.simulations.performance;

import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * A concrete simulation that runs a performance scenario on the
 * full, data-driven topology generated from GeoNames data using region-based brokers.
 * Parameters are now set in main().
 */
public class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    /**
     * Constructor for the region-based GeoNames simulation.
     * @param numberOfReplicas Total number of publisher replicas.
     * @param subscribersPerReplica Number of subscribers for every replica.
     * @param subscriptionRegionSize The size of subscription regions.
     * @param remoteInterestProbability Probability of subscribing to a remote DC.
     */
    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability);
    }
    

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Region-Based) ---");
        
        // --- EASILY ADJUSTABLE PARAMETERS ---
        int simNumberOfReplicas = 20;         // Number of Data Center Replicas
        int simSubscribersPerReplica = 2500;  // Ratio: 2.5k subs per replica (Total = 50,000)
        double simSubscriptionRegionSize = 1.0;  // Decimal degrees (approx. 111km)
        double simRemoteInterestProbability = 0.1; // 10% chance of remote interest

        // --- Create Configuration and Factory ---
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        // --- Create and Run the Simulation ---
        GeoNamesBasedRegionPerformanceSimulation simulation = new GeoNamesBasedRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, simSubscriptionRegionSize, simRemoteInterestProbability
        );
        
        simulation.run(factory, config);
    }
}
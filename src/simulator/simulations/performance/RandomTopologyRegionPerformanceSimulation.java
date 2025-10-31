package simulator.simulations.performance;

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
        int simNumberOfReplicas = 10;           // Number of Data Center Replicas
        int simSubscribersPerReplica = 100;     // Ratio: 100 subs per replica (Total = 10 * 100 = 1000)
        double simSubscriptionRegionSize = 10.0;
        double simRemoteInterestProbability = 0.0; //0.2;

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(4, 3, 5, 0, 0);
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new RegionBrokerFactory());

        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, simSubscriptionRegionSize, simRemoteInterestProbability
        );

        simulation.run(factory, config);
    }
}
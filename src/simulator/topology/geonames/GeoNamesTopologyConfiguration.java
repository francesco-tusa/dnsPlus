package simulator.topology.geonames;

import java.util.logging.Logger;
import simulator.config.SimConfiguration;
import simulator.topology.TopologyConfiguration;

public class GeoNamesTopologyConfiguration implements TopologyConfiguration {

    private final String topologyFilePath;

    // Workload Parameters (Cached here for the simulation runner)
    private final int numberOfReplicas;
    private final int subscribersPerReplica;
    private final int totalSubscribers;
    private final double subscriptionRegionSize;
    private final double remoteInterestProbability;
    private final boolean enableVerboseLogs;

    public GeoNamesTopologyConfiguration() {
        SimConfiguration config = SimConfiguration.get();
        this.topologyFilePath = config.paths.getActiveTopologyFile();

        this.numberOfReplicas = config.workload.numberOfReplicas;
        this.subscribersPerReplica = config.workload.subscribersPerReplica;
        this.totalSubscribers = numberOfReplicas * subscribersPerReplica;
        this.subscriptionRegionSize = config.workload.subscriptionRegionSize;
        this.remoteInterestProbability = config.workload.remoteInterestProbability;
        this.enableVerboseLogs = config.paths.enableVerboseLogs;
    }

    public String getTopologyFilePath() {
        return topologyFilePath;
    }

    // Getters for Simulation Runner
    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public int getSubscribersPerReplica() {
        return subscribersPerReplica;
    }

    public int getTotalSubscribers() {
        return totalSubscribers;
    }

    public double getSubscriptionRegionSize() {
        return subscriptionRegionSize;
    }

    public double getRemoteInterestProbability() {
        return remoteInterestProbability;
    }

    public boolean isEnableVerboseLogs() {
        return enableVerboseLogs;
    }

    @Override
    public void logDetails(Logger logger) {
        logger.info(String.format("Topology Setup (GeoNames): File=%s", topologyFilePath));
        logger.info(String.format("Workload Config: %d Replicas, %d Subs/Rep, RegionSize=%.2f, RemoteProb=%.2f",
                numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability));
    }
}
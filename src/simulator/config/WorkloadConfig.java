package simulator.config;

import java.util.Properties;

public class WorkloadConfig {
    public final int numberOfReplicas;
    public final int subscribersPerReplica;
    public final double subscriptionRegionSize;
    public final double remoteInterestProbability;

    public enum ArrivalDistribution { UNIFORM, POISSON }
    public final ArrivalDistribution arrivalDistribution;
    public final double meanSubscriptionsPerSubscriber;
    
    // External Factor Skew
    public final boolean enableDensitySkew;

    // Spatial Jitter
    public final double locationJitter;

    // for Random or Grid Simulations
    public final int subscribersPerLeafNode;
    public final int publishersPerLeafNode;

    public WorkloadConfig(Properties props) {
        this.numberOfReplicas = ConfigParser.parseInt(props, "workload.replicas", 20);
        this.subscribersPerReplica = ConfigParser.parseInt(props, "workload.subscribersPerReplica", 2500);
        this.subscriptionRegionSize = ConfigParser.parseDouble(props, "workload.subscriptionRegionSize", 10.0);
        this.remoteInterestProbability = ConfigParser.parseDouble(props, "workload.remoteInterestProb", 0.1);

        this.arrivalDistribution = ConfigParser.parseEnum(props, "workload.arrivalDistribution", ArrivalDistribution.class, ArrivalDistribution.POISSON);
        
        this.meanSubscriptionsPerSubscriber = ConfigParser.parseDouble(props, "workload.meanSubscriptionsPerSubscriber", 1.0);
        this.enableDensitySkew = ConfigParser.parseBoolean(props, "workload.enableDensitySkew", false);
        this.locationJitter = ConfigParser.parseDouble(props, "workload.locationJitter", 0.0); 

        // Load Random or Simulation Specifics
        this.subscribersPerLeafNode = ConfigParser.parseInt(props, "workload.subscribersPerLeaf", 5);
        this.publishersPerLeafNode = ConfigParser.parseInt(props, "workload.publishersPerLeaf", 1);
    }
    
    public long getTotalSubscribers() {
        return (long) numberOfReplicas * subscribersPerReplica;
    }
}
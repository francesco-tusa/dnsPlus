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
    
    // External Factor Skew (Activity Multiplier for high-density areas)
    // If true, users in denser regions will send MORE subscriptions than users in sparse regions.
    public final boolean enableDensitySkew;

    // Spatial Jitter (Standard Deviation in degrees)
    // Adds random noise to subscription coordinates to simulate user movement.
    // Recommended: 0.01 - 0.05
    public final double locationJitter;

    // for Random or Grid Simulations
    public final int subscribersPerLeafNode;
    public final int publishersPerLeafNode;

    public WorkloadConfig(Properties props) {
        this.numberOfReplicas = parseInt(props, "workload.replicas", "20");
        this.subscribersPerReplica = parseInt(props, "workload.subscribersPerReplica", "2500");
        this.subscriptionRegionSize = parseDouble(props, "workload.subscriptionRegionSize", "10.0");
        this.remoteInterestProbability = parseDouble(props, "workload.remoteInterestProb", "0.1");

        String distStr = props.getProperty("workload.arrivalDistribution", "POISSON").toUpperCase();
        this.arrivalDistribution = ArrivalDistribution.valueOf(distStr);
        
        this.meanSubscriptionsPerSubscriber = parseDouble(props, "workload.meanSubscriptionsPerSubscriber", "1.0");
        this.enableDensitySkew = Boolean.parseBoolean(props.getProperty("workload.enableDensitySkew", "false"));
        this.locationJitter = parseDouble(props, "workload.locationJitter", "0.0"); // Default: No jitter

        // Load Random or Simulation Specifics
        this.subscribersPerLeafNode = parseInt(props, "workload.subscribersPerLeaf", "5");
        this.publishersPerLeafNode = parseInt(props, "workload.publishersPerLeaf", "1");
    }
    
    
    public long getTotalSubscribers() {
        return (long) numberOfReplicas * subscribersPerReplica;
    }
    
    private int parseInt(Properties props, String key, String defaultVal) {
        try { return Integer.parseInt(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Config Error: Invalid integer for " + key); }
    }
    
    private double parseDouble(Properties props, String key, String defaultVal) {
        try { return Double.parseDouble(props.getProperty(key, defaultVal)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Config Error: Invalid double for " + key); }
    }
}
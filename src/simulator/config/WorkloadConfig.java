package simulator.config;

import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class WorkloadConfig {
    private static final Logger logger = CustomLogger.getLogger(WorkloadConfig.class.getName());
    
    public final int numberOfReplicas; 
    
    public final double subscribersPerReplica;
    public final double subscriptionRegionSize;
    public final double remoteInterestProbability;
    public final ArrivalDistribution arrivalDistribution;
    public final double meanSubscriptionsPerSubscriber;
    public final boolean enableDensitySkew;
    public final double locationJitter;
    public final int subscribersPerLeafNode;
    public final int publishersPerLeafNode;
    
    public final boolean enableGroundTruth;

    public enum ArrivalDistribution { UNIFORM, POISSON }

    // Internal flag for Batch Experiments
    private final int explicitSubscriberCount;

    public WorkloadConfig(Properties props) {
        this.numberOfReplicas = ConfigParser.parseInt(props, "workload.replicas", 20);
        this.explicitSubscriberCount = ConfigParser.parseInt(props, "workload.subscribers.count", -1);
        
        this.subscribersPerReplica = ConfigParser.parseDouble(props, "workload.subscribersPerReplica", 2500);
        this.subscriptionRegionSize = ConfigParser.parseDouble(props, "workload.subscriptionRegionSize", 10.0);
        this.remoteInterestProbability = ConfigParser.parseDouble(props, "workload.remoteInterestProb", 0.1);
        this.arrivalDistribution = ConfigParser.parseEnum(props, "workload.arrivalDistribution", ArrivalDistribution.class, ArrivalDistribution.POISSON);
        this.meanSubscriptionsPerSubscriber = ConfigParser.parseDouble(props, "workload.meanSubscriptionsPerSubscriber", 1.0);
        this.enableDensitySkew = ConfigParser.parseBoolean(props, "workload.enableDensitySkew", false);
        this.locationJitter = ConfigParser.parseDouble(props, "workload.locationJitter", 0.0); 
        this.subscribersPerLeafNode = ConfigParser.parseInt(props, "workload.subscribersPerLeaf", 5);
        this.publishersPerLeafNode = ConfigParser.parseInt(props, "workload.publishersPerLeaf", 1);
        this.enableGroundTruth = ConfigParser.parseBoolean(props, "workload.enableGroundTruth", true);
    }
    
    public boolean isBatchMode() {
        return explicitSubscriberCount > -1;
    }

    public int getTotalSubscribers() {
        if (explicitSubscriberCount > -1) {
            return explicitSubscriberCount;
        }
        return (int) (numberOfReplicas * subscribersPerReplica);
    }

    public int getTotalPublishers() {
        return numberOfReplicas; 
    }
}
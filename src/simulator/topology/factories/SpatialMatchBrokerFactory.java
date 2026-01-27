package simulator.topology.factories;

import java.util.logging.Logger;
import utils.CustomLogger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SpatialMatchLeafBroker;
import simulator.regions.policy.RegionIntersectionPropagationPolicy;
import simulator.regions.policy.PropagationRegionPolicy;
import simulator.regions.policy.StrictPropagationPolicy;

public class SpatialMatchBrokerFactory implements BoundedBrokerFactory {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchBrokerFactory.class.getName());

    private final boolean forceSingleRegion; 
    private final double smartThreshold;
    private final PropagationRegionPolicy propagationPolicy;

    // Default Constructor (Uses Config File)
    public SpatialMatchBrokerFactory() {
        SimConfiguration config = SimConfiguration.get();
        BrokerConfig brokerConfig = config.broker;
        
        if (brokerConfig.strategy == BrokerConfig.StrategyType.SIMPLE) {
            this.forceSingleRegion = true;
            this.smartThreshold = 0.0;
        } else {
            this.forceSingleRegion = false;
            this.smartThreshold = brokerConfig.getSmartThreshold();
        }

        if (brokerConfig.isIntersectionOptimizationEnabled()) {
            this.propagationPolicy = new RegionIntersectionPropagationPolicy();
        } else {
            this.propagationPolicy = new StrictPropagationPolicy();
        }
        logMode("Global Config");
    }
    
    // Test Constructor 1
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
        this.propagationPolicy = new StrictPropagationPolicy();
        logMode("Manual Override (Strict)");
    }
    
    // Test Constructor 2 (Used by RegressionSuiteRunner)
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold, boolean enableClipping) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
        this.propagationPolicy = enableClipping ? new RegionIntersectionPropagationPolicy() : new StrictPropagationPolicy();
        logMode("Manual Override (Custom Clip)");
    }

    private void logMode(String source) {
        String mode = forceSingleRegion ? "SIMPLE (Clip/SingleRegion)" : "SMART (Merge/MultiRegion)";
        String policy = (propagationPolicy instanceof RegionIntersectionPropagationPolicy) ? "IntersectionOpt" : "Strict";
        logger.info(String.format("Factory Init [%s]: Mode=%s, Policy=%s", source, mode, policy));
    }

    @Override
    public BoundedBroker createBroker(String name) {
        return new SpatialMatchBroker(name, forceSingleRegion, smartThreshold, propagationPolicy);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        return new SpatialMatchLeafBroker(name, p1, p2, forceSingleRegion, smartThreshold, propagationPolicy);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new SpatialMatchLeafBroker(name, forceSingleRegion, smartThreshold, propagationPolicy);
    }
}
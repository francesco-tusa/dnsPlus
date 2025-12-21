package simulator.topology.factories;

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

    private final boolean forceSingleRegion; 
    private final double smartThreshold;
    private final PropagationRegionPolicy propagationPolicy;

    public SpatialMatchBrokerFactory() {
        SimConfiguration config = SimConfiguration.get();
        BrokerConfig brokerConfig = config.broker;
        
        // 1. Configure Storage Strategy (Simple vs Smart)
        if (brokerConfig.strategy == BrokerConfig.StrategyType.SIMPLE) {
            this.forceSingleRegion = true;
            this.smartThreshold = 0.0; // Irrelevant for Simple Store
        } else {
            // Configuration for SMART Strategy
            this.forceSingleRegion = false;
            this.smartThreshold = brokerConfig.getSmartThreshold();
        }

        // 2. Configure Propagation Policy (Strict vs Clipping)
        // This applies to BOTH Simple and Smart strategies based on the config flag.
        if (brokerConfig.isIntersectionOptimizationEnabled()) {
            this.propagationPolicy = new RegionIntersectionPropagationPolicy();
        } else {
            this.propagationPolicy = new StrictPropagationPolicy();
        }
    }
    
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
        this.propagationPolicy = new StrictPropagationPolicy();
    }
    
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold, boolean enableClipping) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
        this.propagationPolicy = enableClipping ? new RegionIntersectionPropagationPolicy() : new StrictPropagationPolicy();
    }

    @Override
    public BoundedBroker createBroker(String name) {
        // The policy passed here will now be the one selected by the config flag
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
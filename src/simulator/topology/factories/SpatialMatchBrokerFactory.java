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
        
        if (brokerConfig.strategy == BrokerConfig.StrategyType.SIMPLE) {
            // --- Configuration for SIMPLE Strategy ---
            this.forceSingleRegion = true;
            this.smartThreshold = 0.0; // Irrelevant for Simple Store
            
            // Select Policy: Intersection (Optimized) or Strict (Default)
            if (brokerConfig.isIntersectionOptimizationEnabled()) {
                this.propagationPolicy = new RegionIntersectionPropagationPolicy();
            } else {
                this.propagationPolicy = new StrictPropagationPolicy();
            }
            
        } else {
            // --- Configuration for SMART Strategy ---
            this.forceSingleRegion = false;
            this.smartThreshold = brokerConfig.getSmartThreshold();
            
            // SMART always uses Strict policy (no MBR clipping allowed)
            this.propagationPolicy = new StrictPropagationPolicy();
        }
    }
    
    // Manual/Test Constructor
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
        this.propagationPolicy = new StrictPropagationPolicy();
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
package marketplace.topology.store;

import simulator.core.TreeNode;
import simulator.regions.SubscriptionWithRegion;
import simulator.regions.store.StoreUpdate;
import utils.CustomLogger;

import java.util.logging.Logger;

import marketplace.common.MetricHyperCube;
import marketplace.events.ServiceOffer;

public class HypercubeRegionStore extends AbstractMarketplaceRegionStore {

    private static final Logger logger = CustomLogger.getLogger(HypercubeRegionStore.class.getName());
    public HypercubeRegionStore(double threshold) {
        super(threshold);
    }

    @Override
    public StoreUpdate addOrUpdate(TreeNode source, SubscriptionWithRegion sub) {
        if (sub instanceof ServiceOffer && !(sub.getRegion() instanceof MetricHyperCube)) {
            logger.warning("FaaS Offer missing MetricHyperCube! Type: " + sub.getRegion().getClass().getSimpleName());
        }
        return super.addOrUpdate(source, sub);
    }

    @Override
    protected boolean shouldMerge(simulator.regions.Region accumulator, SubscriptionWithRegion existing) {
        if (!(accumulator instanceof MetricHyperCube) || !(existing.getRegion() instanceof MetricHyperCube)) {
            // Fallback for standard DNS++ packets
            return super.shouldMerge(accumulator, existing); 
        }

        MetricHyperCube accCube = (MetricHyperCube) accumulator;
        MetricHyperCube extCube = (MetricHyperCube) existing.getRegion();

        // 1. Calculate Spatial FPR using the shared base class utility
        double spatialFpr = calculateSpatialFpr(accumulator, existing);

        // 2. Calculate N-Dimensional QoS FPR
        double[] accMin = accCube.getMinValues();
        double[] accMax = accCube.getMaxValues();
        double[] extMin = extCube.getMinValues();
        double[] extMax = extCube.getMaxValues();

        double qosFpr = 0.0;
        for (int i = 0; i < accMin.length; i++) {
            double currentMax = Math.max(1e-6, accMax[i] - accMin[i]);
            double newMax = Math.max(1e-6, extMax[i] - extMin[i]);
            
            double mergedMin = Math.min(accMin[i], extMin[i]);
            double mergedMax = Math.max(accMax[i], extMax[i]);
            double mergedVol = Math.max(1e-6, mergedMax - mergedMin);

            double dimFpr = 1.0 - ((currentMax + newMax) / mergedVol);
            qosFpr = Math.max(qosFpr, dimFpr);
        }

        // 3. Strict decoupled validation
        double combinedFpr = Math.max(spatialFpr, qosFpr);
        return combinedFpr <= this.getThreshold();
    }
}
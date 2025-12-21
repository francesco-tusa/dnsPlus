package simulator.regions.policy;

import simulator.regions.BoundedBroker;
import simulator.regions.SpatialRegion;

public class RegionIntersectionPropagationPolicy implements PropagationRegionPolicy {

    @Override
    public SpatialRegion determineRegionToSend(BoundedBroker child, SpatialRegion demand) {
        // 1. Saturation Optimization
        if (child.isSaturatedByParent()) {
            return null; 
        }

        // 2. Intersection (Clipping)
        SpatialRegion intersection = child.getRegion().intersection(demand);

        // 3. Update Saturation Status
        if (intersection != null) {
            if (intersection.contains(child.getRegion())) {
                child.setSaturatedByParent(true);
            }
        }

        return intersection;
    }

    @Override
    public String getDescription() {
        return "Clipping & Saturation (Intersection Policy)";
    }
}
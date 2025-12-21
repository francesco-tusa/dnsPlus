package simulator.regions.policy;

import simulator.regions.BoundedBroker;
import simulator.regions.SpatialRegion;

public class StrictPropagationPolicy implements PropagationRegionPolicy {

    @Override
    public SpatialRegion determineRegionToSend(BoundedBroker child, SpatialRegion demand) {
        if (child.getRegion().intersects(demand)) {
            return demand;
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "N/A (Strict Mode)";
    }
}
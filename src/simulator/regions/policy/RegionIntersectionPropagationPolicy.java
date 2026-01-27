package simulator.regions.policy;

import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.regions.SpatialRegion;

public class RegionIntersectionPropagationPolicy implements PropagationRegionPolicy {

    @Override
    public SpatialRegion determineRegionToSend(BoundedBroker child, SpatialRegion demand) {
        if (child.isSaturatedByParent()) return null; 

        SpatialRegion childReg = child.getRegion();
        if (childReg == null || childReg.getBottomLeft() == null) return null;

        Region d = (Region) demand;
        Region c = (Region) childReg;

        float dMinL = (float)d.getMinLon(), dMaxL = (float)d.getMaxLon();
        float dMinT = (float)d.getMinLat(), dMaxT = (float)d.getMaxLat();
        float cMinL = (float)c.getMinLon(), cMaxL = (float)c.getMaxLon();
        float cMinT = (float)c.getMinLat(), cMaxT = (float)c.getMaxLat();

    // 1. Saturation Check (Containment)
        if (Region.fastContains(dMinL, dMaxL, dMinT, dMaxT, cMinL, cMaxL, cMinT, cMaxT)) {
            child.setSaturatedByParent(true);
            return childReg; 
        }

    // 2. GRID FIX: Use fastIntersects instead of area > 0
        if (!Region.fastIntersects(dMinL, dMaxL, dMinT, dMaxT, cMinL, cMaxL, cMinT, cMaxT)) {
            return null;
        }

        // 3. Clipping (Only materialize when intersection is confirmed)
        return demand.intersection(childReg);
    }
}
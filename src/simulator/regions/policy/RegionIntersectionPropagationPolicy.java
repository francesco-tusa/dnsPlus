package simulator.regions.policy;

import simulator.regions.SpatialRegion;

/**
 * Implements the Intersection (Clipping) optimization.
 * Calculates the geometric intersection between the global need and the child's capability.
 * This effectively implements "Update Suppression" and prevents "Gap" issues.
 */
public class RegionIntersectionPropagationPolicy implements PropagationRegionPolicy {
    @Override
    public SpatialRegion determineRegionToSend(SpatialRegion childRegion, SpatialRegion candidateRegion) {
        // Returns the intersection (clipped region), or null if they don't intersect.
        if (childRegion == null) return null;
        return candidateRegion.intersection(childRegion); 
    }
}
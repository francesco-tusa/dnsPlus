package simulator.regions.policy;

import simulator.regions.SpatialRegion;

/**
 * Implements the strict SIMPLE algorithm behavior.
 * If the input overlaps the child, send the FULL aggregated input.
 */
public class StrictPropagationPolicy implements PropagationRegionPolicy {
    @Override
    public SpatialRegion determineRegionToSend(SpatialRegion childRegion, SpatialRegion candidateRegion) {
        if (childRegion != null && childRegion.intersects(candidateRegion)) {
            // Strictly adhere to algorithm: "keep the entire subscription region"
            return candidateRegion; 
        }
        return null;
    }
}
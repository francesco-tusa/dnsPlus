package simulator.regions.policy;

import simulator.regions.SpatialRegion;

/**
 * Strategy interface to determine the region that should be propagated to a child.
 */
public interface PropagationRegionPolicy {
    /**
     * Determines the region to send downwards based on the aggregated input and the child's capability.
     * @param childRegion The region covered by the child broker.
     * @param candidateRegion The aggregated region from the input store (The Global Requirement).
     * @return The region to propagate, or null if no propagation should occur.
     */
    SpatialRegion determineRegionToSend(SpatialRegion childRegion, SpatialRegion candidateRegion);
}
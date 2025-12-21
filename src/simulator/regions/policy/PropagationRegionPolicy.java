package simulator.regions.policy;

import simulator.regions.BoundedBroker;
import simulator.regions.SpatialRegion;

public interface PropagationRegionPolicy {
    /**
     * Determines the spatial region to send to a child broker.
     */
    SpatialRegion determineRegionToSend(BoundedBroker child, SpatialRegion demand);

    /**
     * Returns a human-readable description of this policy/strategy.
     * Used for logging and configuration verification.
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}
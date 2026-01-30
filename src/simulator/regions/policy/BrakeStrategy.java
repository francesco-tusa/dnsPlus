package simulator.regions.policy;

import simulator.core.Location;
import simulator.regions.Region;

public interface BrakeStrategy {
    /**
     * Determines if a publication should be propagated upstream.
     * @param pubLocation The location of the publication.
     * @param region The broker's region (used to calculate center and quadrants).
     * @return true if allowed, false if blocked.
     */
    boolean shouldPropagate(Location pubLocation, Region region);
}
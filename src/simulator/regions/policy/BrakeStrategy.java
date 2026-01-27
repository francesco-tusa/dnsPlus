package simulator.regions.policy;

import simulator.core.Location;

public interface BrakeStrategy {
    /**
     * Determines if a publication should be propagated upstream.
     * @param pubLocation The location of the publication.
     * @param regionCenter The center of the broker's region.
     * @param currentTimestamp The current simulation time (ms).
     * @return true if allowed, false if blocked.
     */
    boolean shouldPropagate(Location pubLocation, Location regionCenter, long currentTimestamp);
}
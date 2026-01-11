package simulator.regions.policy;

import simulator.core.Location;

public class NoOpBrakeStrategy implements BrakeStrategy {
    @Override
    public boolean shouldPropagate(Location pubLocation, Location regionCenter, long currentTimestamp) {
        return true; // Always allow
    }
}
package simulator.regions.policy;

import simulator.core.Location;
import simulator.regions.Region;

public class NoOpBrakeStrategy implements BrakeStrategy {
    @Override
    public boolean shouldPropagate(Location pubLocation, Region region) {
        return true; 
    }
}
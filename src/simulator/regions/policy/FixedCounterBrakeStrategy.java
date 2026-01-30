package simulator.regions.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.regions.Region;
import utils.CustomLogger;

/**
 * A brake strategy that allows a fixed number of publications per quadrant
 * and then permanently blocks further propagation.
 */
public class FixedCounterBrakeStrategy implements BrakeStrategy {

    // 1. Initialize Logger using the project's utility
    private static final Logger logger = CustomLogger.getLogger(FixedCounterBrakeStrategy.class.getName());
    
    private final int maxTokens;
    private final Map<Integer, Integer> quadrantTokens; 

    public FixedCounterBrakeStrategy(int maxTokens) {
        this.maxTokens = maxTokens;
        this.quadrantTokens = new HashMap<>();
        resetTokens();
    }

    @Override
    public boolean shouldPropagate(Location pubLocation, Region region) {
        if (region == null) return true;

        int quadrant = Region.getQuadrant(pubLocation, region);
        
        int tokens = quadrantTokens.getOrDefault(quadrant, 0);

        if (tokens > 0) {
            int remaining = tokens - 1;
            quadrantTokens.put(quadrant, remaining);

            // 2. Log saturation event: Triggered only when the last token is used.
            if (remaining == 0) {
                logger.fine(String.format(
                    "[BRAKE SATURATED] Quadrant %d in Region %s has reached the limit of %d publications. Further propagation blocked.", 
                    quadrant, region.toString(), maxTokens
                ));
            }

            return true;
        } else {
            return false;
        }
    }

    private void resetTokens() {
        for (int i = 0; i < 4; i++) {
            quadrantTokens.put(i, maxTokens);
        }
    }
}
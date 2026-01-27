package simulator.regions.policy;

import java.util.HashMap;
import java.util.Map;
import simulator.core.Location;

public class DecayingCounterBrakeStrategy implements BrakeStrategy {

    private final int maxTokens;
    private final long resetIntervalMs;
    
    // Key: Quadrant ID (0-3), Value: Tokens Left
    private final Map<Integer, Integer> quadrantTokens; 
    private long lastResetTime = 0;

    public DecayingCounterBrakeStrategy(int maxTokens, long intervalMs) {
        this.maxTokens = maxTokens;
        this.resetIntervalMs = intervalMs;
        this.quadrantTokens = new HashMap<>();
        this.lastResetTime = System.currentTimeMillis(); 
        resetTokens();
    }

    @Override
    public boolean shouldPropagate(Location pubLocation, Location regionCenter, long currentTimestamp) {
        // 1. Lazy Reset Check
        if (currentTimestamp - lastResetTime > resetIntervalMs) {
            resetTokens();
            lastResetTime = currentTimestamp;
        }

        // 2. Determine Quadrant (NE, NW, SW, SE)
        int quadrant = getQuadrant(pubLocation, regionCenter);
        int tokens = quadrantTokens.getOrDefault(quadrant, 0);

        // 3. Token Check
        if (tokens > 0) {
            quadrantTokens.put(quadrant, tokens - 1);
            return true; // Allowed
        } else {
            return false; // Dropped
        }
    }

    private void resetTokens() {
        for (int i = 0; i < 4; i++) {
            quadrantTokens.put(i, maxTokens);
        }
    }

    private int getQuadrant(Location p, Location center) {
        // API Alignment: getX()=Longitude, getY()=Latitude
        boolean north = p.getY() >= center.getY();
        boolean east  = p.getX() >= center.getX();

        if (north && east) return 0;
        if (north && !east) return 1;
        if (!north && !east) return 2;
        return 3; 
    }
}
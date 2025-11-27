package simulator.simulations.performance.metrics;

import java.util.List;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(GroundTruthCalculator.class.getName());

    public static long calculateRegionMatches(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        logger.info("");
        logger.info("--- Calculating Ground Truth Matches ---");

        long matches = 0;
        if (subs.isEmpty() || pubs.isEmpty()) return 0;

        long interval = Math.max(1, (long)pubs.size() * subs.size() / 10_000_000); 

        for (int i = 0; i < pubs.size(); i++) {
            Location pubLoc = pubs.get(i).getLocation();
            for (SubscriptionWithRegion sub : subs) {
                if (sub.getRegion().contains(pubLoc)) {
                    matches++;
                }
            }
            if ((i + 1) % interval == 0) {
                logger.info(String.format("  ... checked %d / %d publications.", (i + 1), pubs.size()));
            }
        }
        logger.info("--- Ground Truth Calculation Complete: " + matches + " total potential matches. ---");
        return matches;
    }
}
package simulator.simulations.performance.metrics.groundtruth;

import java.util.List;
import java.util.concurrent.ExecutorService;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;

/**
 * Strategy interface for calculating the theoretical "Ground Truth" (ideal) delivery count.
 * Implementations define what constitutes a valid match (e.g., spatial intersection vs. temporal proximity).
 */
public interface GroundTruthCalculator {
    /**
     * Calculates the total number of valid notifications that SHOULD occur for the given batch
     * of subscriptions against the provided publications.
     *
     * @param subs The list of subscriptions (can be Region-based or Location-based).
     * @param pubs The list of publications to check against.
     * @param executor A shared executor service to parallelize calculations.
     * @return The count of valid matches/updates.
     */
    long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs, ExecutorService executor);
}
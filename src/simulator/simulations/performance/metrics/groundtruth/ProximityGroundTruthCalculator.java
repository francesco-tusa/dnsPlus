package simulator.simulations.performance.metrics.groundtruth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import utils.CustomLogger;

public class ProximityGroundTruthCalculator implements GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(ProximityGroundTruthCalculator.class.getName());

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs) {
        if (subs.isEmpty() || pubs.isEmpty()) return 0;

        // 1. Filter and Cast to SubscriptionWithLocation
        List<SubscriptionWithLocation> locSubs = new ArrayList<>(subs.size());
        for (SimulationSubscription s : subs) {
            if (s instanceof SubscriptionWithLocation) {
                locSubs.add((SubscriptionWithLocation) s);
            }
        }

        if (locSubs.isEmpty()) return 0;

        logger.fine("--- Calculating Proximity Ground Truth (Necessary Updates) ---");

        // 2. Parallel Strategy: Partition SUBSCRIBERS
        // We cannot partition Publications effectively because the order matters (temporal state).
        // Each subscriber must process the full stream of pubs to update their "best distance".
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long totalNecessaryUpdates = 0;

        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            int batchSize = (int) Math.ceil((double) locSubs.size() / numThreads);

            for (int i = 0; i < locSubs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, locSubs.size());
                List<SubscriptionWithLocation> subBatch = locSubs.subList(i, end);

                tasks.add(() -> {
                    long localCount = 0;
                    
                    for (SubscriptionWithLocation sub : subBatch) {
                        double bestDistSq = Double.MAX_VALUE;
                        
                        // STRICT ORDER REQUIRED: Iterate publications in generation/send order
                        for (PublicationWithLocation pub : pubs) {
                            // Assuming SubscriptionWithLocation has a getLocation() method
                            double d = sub.getLocation().distanceSquared(pub.getLocation());
                            
                            if (d < bestDistSq) {
                                bestDistSq = d;
                                localCount++; // This is a necessary update (strict improvement)
                            }
                        }
                    }
                    return localCount;
                });
            }

            for (Future<Long> result : executor.invokeAll(tasks)) {
                totalNecessaryUpdates += result.get();
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Parallel proximity truth failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        logger.fine("--- Proximity Ground Truth Complete: " + totalNecessaryUpdates + " necessary updates. ---");
        return totalNecessaryUpdates;
    }
}
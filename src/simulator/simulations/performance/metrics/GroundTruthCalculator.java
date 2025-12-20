package simulator.simulations.performance.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(GroundTruthCalculator.class.getName());

    public static long calculateRegionMatches(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        if (subs.isEmpty() || pubs.isEmpty()) return 0;

        int numThreads = Runtime.getRuntime().availableProcessors();

        // For small datasets, avoid the overhead of thread management
        if (pubs.size() < 500 || numThreads <= 1) {
            return calculateSequential(subs, pubs);
        }

        logger.info("");
        logger.info(String.format("--- Calculating Ground Truth Matches (Parallel - %d Threads) ---", numThreads));

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Long>> tasks = new ArrayList<>();

        // Partition the publications list into batches for each thread
        int batchSize = (int) Math.ceil((double) pubs.size() / numThreads);

        for (int i = 0; i < pubs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, pubs.size());
            // Create a sublist view (safe for read-only access in threads)
            List<PublicationWithLocation> batch = pubs.subList(i, end);
            
            // Create a task for this batch
            tasks.add(() -> countMatchesInBatch(batch, subs));
        }

        long totalMatches = 0;
        try {
            // Execute all tasks and wait for them to complete
            List<Future<Long>> results = executor.invokeAll(tasks);
            
            // Aggregate the results
            for (Future<Long> result : results) {
                totalMatches += result.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Parallel ground truth calculation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        logger.info("--- Ground Truth Calculation Complete: " + totalMatches + " unique subscriber matches. ---");
        return totalMatches;
    }

    // Standard sequential execution (fallback)
    private static long calculateSequential(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        logger.info("");
        logger.info("--- Calculating Ground Truth Matches (Sequential) ---");
        long matches = countMatchesInBatch(pubs, subs);
        logger.info("--- Ground Truth Calculation Complete: " + matches + " unique subscriber matches. ---");
        return matches;
    }

    /**
     * Core logic to count matches for a specific batch of publications.
     * Thread-safe as it uses local variables and read-only access to lists.
     */
    private static long countMatchesInBatch(List<PublicationWithLocation> batch, List<SubscriptionWithRegion> subs) {
        long localMatches = 0;

        for (PublicationWithLocation pub : batch) {
            Location pubLoc = pub.getLocation();
            
            // Use a Set to ensure we only count each unique Subscriber ONCE per publication
            Set<TreeNode> matchedSubscribers = new HashSet<>();

            for (SubscriptionWithRegion sub : subs) {
                if (sub.getRegion().contains(pubLoc)) {
                    // Identify the subscriber (Source node)
                    if (sub.getSource() != null) {
                        matchedSubscribers.add(sub.getSource());
                    }
                }
            }
            
            // Add the count of unique interested subscribers for this publication
            localMatches += matchedSubscribers.size();
        }
        return localMatches;
    }
}
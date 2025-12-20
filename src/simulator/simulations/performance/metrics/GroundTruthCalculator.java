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

    // THRESHOLD: If (Pubs * Subs) < 100,000, it is faster to run sequentially
    // than to pay the "tax" of spinning up threads.
    private static final long MIN_WORKLOAD_THRESHOLD = 100_000; 

    public static long calculateRegionMatches(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        if (subs.isEmpty() || pubs.isEmpty()) return 0;

        int numThreads = Runtime.getRuntime().availableProcessors();
        long workload = (long) pubs.size() * subs.size();

        // 1. SEQUENTIAL: Use main thread if data is small or we only have 1 core.
        if (workload < MIN_WORKLOAD_THRESHOLD || numThreads <= 1) {
            return calculateSequential(subs, pubs);
        }

        // 2. PARALLEL: Choose the best strategy based on data shape
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long totalMatches = 0;

        try {
            // DECISION:
            // If we have enough publications to give every core at least 1 item,
            // we use Publisher Parallelization (Strategy A) because it's faster (no merging).
            // Otherwise, we must parallelize the Subscribers (Strategy B).
            if (pubs.size() >= numThreads) {
                totalMatches = executePublisherParallel(executor, subs, pubs, numThreads);
            } else {
                totalMatches = executeSubscriberParallel(executor, subs, pubs, numThreads);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Parallel ground truth calculation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return totalMatches;
    }

    /**
     * STRATEGY A: Publisher Parallelization (Preferred)
     * Fastest. Zero synchronization needed during calculation.
     */
    private static long executePublisherParallel(ExecutorService executor, 
                                                 List<SubscriptionWithRegion> subs, 
                                                 List<PublicationWithLocation> pubs, 
                                                 int numThreads) throws InterruptedException, ExecutionException {
        logger.info(String.format("--- Parallel Strategy A: Partitioning %d Publications across %d Threads ---", pubs.size(), numThreads));
        
        List<Callable<Long>> tasks = new ArrayList<>();
        int batchSize = (int) Math.ceil((double) pubs.size() / numThreads);

        for (int i = 0; i < pubs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, pubs.size());
            List<PublicationWithLocation> batch = pubs.subList(i, end);
            tasks.add(() -> countMatchesInBatch(batch, subs));
        }

        long total = 0;
        for (Future<Long> result : executor.invokeAll(tasks)) {
            total += result.get();
        }
        logger.info("--- Ground Truth Calculation Complete: " + total + " unique subscriber matches. ---");
        return total;
    }

    /**
     * STRATEGY B: Subscriber Parallelization (Fallback)
     * Used when we have very few publications (e.g. < 8) but millions of subscribers.
     */
    private static long executeSubscriberParallel(ExecutorService executor, 
                                                  List<SubscriptionWithRegion> subs, 
                                                  List<PublicationWithLocation> pubs, 
                                                  int numThreads) throws InterruptedException, ExecutionException {
        logger.info(String.format("--- Parallel Strategy B: Partitioning %d Subscribers across %d Threads ---", subs.size(), numThreads));
        
        long totalMatches = 0;
        int batchSize = (int) Math.ceil((double) subs.size() / numThreads);
        
        for (PublicationWithLocation pub : pubs) {
            List<Callable<Set<TreeNode>>> tasks = new ArrayList<>();
            Location pubLoc = pub.getLocation();

            // Split subscribers into chunks
            for (int i = 0; i < subs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, subs.size());
                List<SubscriptionWithRegion> subBatch = subs.subList(i, end);
                
                tasks.add(() -> {
                    Set<TreeNode> localFound = new HashSet<>();
                    for (SubscriptionWithRegion s : subBatch) {
                        // Check if subscription covers the publication location
                        if (s.getRegion().contains(pubLoc) && s.getSource() != null) {
                            localFound.add(s.getSource());
                        }
                    }
                    return localFound;
                });
            }

            // Reduce: Merge all partial sets for this publication
            Set<TreeNode> uniqueForThisPub = new HashSet<>();
            List<Future<Set<TreeNode>>> results = executor.invokeAll(tasks);
            
            for (Future<Set<TreeNode>> fut : results) {
                uniqueForThisPub.addAll(fut.get());
            }
            
            totalMatches += uniqueForThisPub.size();
        }
        
        logger.info("--- Ground Truth Calculation Complete: " + totalMatches + " unique subscriber matches. ---");
        return totalMatches;
    }

    private static long calculateSequential(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        logger.info("--- Calculating Ground Truth Matches (Sequential) ---");
        long matches = countMatchesInBatch(pubs, subs);
        logger.info("--- Ground Truth Calculation Complete: " + matches + " unique subscriber matches. ---");
        return matches;
    }

    private static long countMatchesInBatch(List<PublicationWithLocation> batch, List<SubscriptionWithRegion> subs) {
        long localMatches = 0;
        for (PublicationWithLocation pub : batch) {
            Location pubLoc = pub.getLocation();
            Set<TreeNode> matchedSubscribers = new HashSet<>();
            for (SubscriptionWithRegion sub : subs) {
                if (sub.getRegion().contains(pubLoc)) {
                    if (sub.getSource() != null) {
                        matchedSubscribers.add(sub.getSource());
                    }
                }
            }
            localMatches += matchedSubscribers.size();
        }
        return localMatches;
    }
}
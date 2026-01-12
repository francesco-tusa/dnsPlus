package simulator.simulations.performance.metrics.groundtruth;

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
import simulator.events.SimulationSubscription;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class RegionGroundTruthCalculator implements GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(RegionGroundTruthCalculator.class.getName());

    // UPDATED: Raised from 100,000 to 1,000,000.
    // 100k operations are too fast to justify the overhead of creating a ThreadPool.
    // In streaming mode (batches of 5000 subs), this prevents "Thread Thrashing".
    private static final long MIN_WORKLOAD_THRESHOLD = 1_000_000;

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs) {
        if (subs.isEmpty() || pubs.isEmpty()) return 0;

        // 1. Filter and Cast to SubscriptionWithRegion
        List<SubscriptionWithRegion> regionSubs = new ArrayList<>(subs.size());
        for (SimulationSubscription s : subs) {
            if (s instanceof SubscriptionWithRegion) {
                regionSubs.add((SubscriptionWithRegion) s);
            }
        }

        if (regionSubs.isEmpty()) return 0;

        // 2. Determine Execution Strategy
        int numThreads = Runtime.getRuntime().availableProcessors();
        long workload = (long) pubs.size() * regionSubs.size();

        // LOGIC UPDATE: Added check for very small publication lists.
        // Even if workload is high, parallelizing 20 pubs is inefficient due to setup costs.
        boolean tooFewPubsForParallel = pubs.size() < numThreads && pubs.size() < 50;

        if (workload < MIN_WORKLOAD_THRESHOLD || numThreads <= 1 || tooFewPubsForParallel) {
            return calculateSequential(regionSubs, pubs);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long totalMatches = 0;

        try {
            // Strategy A: Many Publishers -> Split Publishers across threads
            if (pubs.size() >= numThreads) {
                totalMatches = executePublisherParallel(executor, regionSubs, pubs, numThreads);
            } 
            // Strategy B: Few Publishers, Many Subs -> Split Subs across threads
            else {
                totalMatches = executeSubscriberParallel(executor, regionSubs, pubs, numThreads);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Parallel region ground truth calculation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return totalMatches;
    }

    private long executePublisherParallel(ExecutorService executor,
                                          List<SubscriptionWithRegion> subs,
                                          List<PublicationWithLocation> pubs,
                                          int numThreads) throws InterruptedException, ExecutionException {
        
        logger.fine(String.format("--- Parallel Strategy A: Partitioning %d Publications across %d Threads ---", pubs.size(), numThreads));

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
        
        // Removed explicit log per call to reduce console spam in streaming mode
        return total;
    }

    private long executeSubscriberParallel(ExecutorService executor,
                                           List<SubscriptionWithRegion> subs,
                                           List<PublicationWithLocation> pubs,
                                           int numThreads) throws InterruptedException, ExecutionException {
        
        logger.fine(String.format("--- Parallel Strategy B: Partitioning %d Subscribers across %d Threads ---", subs.size(), numThreads));

        long totalMatches = 0;
        int batchSize = (int) Math.ceil((double) subs.size() / numThreads);

        // Note: Ideally, we would flatten this loop to avoid invoking the executor multiple times,
        // but for "Few Publishers" (e.g. < 50), this inner loop is acceptable IF the subscriber count is massive.
        for (PublicationWithLocation pub : pubs) {
            List<Callable<Set<TreeNode>>> tasks = new ArrayList<>();
            Location pubLoc = pub.getLocation();

            for (int i = 0; i < subs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, subs.size());
                List<SubscriptionWithRegion> subBatch = subs.subList(i, end);

                tasks.add(() -> {
                    Set<TreeNode> localFound = new HashSet<>();
                    for (SubscriptionWithRegion s : subBatch) {
                        if (s.getRegion().contains(pubLoc) && s.getSource() != null) {
                            localFound.add(s.getSource());
                        }
                    }
                    return localFound;
                });
            }

            Set<TreeNode> uniqueForThisPub = new HashSet<>();
            List<Future<Set<TreeNode>>> results = executor.invokeAll(tasks);

            for (Future<Set<TreeNode>> fut : results) {
                uniqueForThisPub.addAll(fut.get());
            }
            totalMatches += uniqueForThisPub.size();
        }
        
        return totalMatches;
    }

    private long calculateSequential(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        // Logging changed to FINEST to avoid spamming logs 1000 times
        logger.finest("--- Calculating Region Ground Truth Matches (Sequential) ---");
        long matches = countMatchesInBatch(pubs, subs);
        return matches;
    }

    private long countMatchesInBatch(List<PublicationWithLocation> batch, List<SubscriptionWithRegion> subs) {
        long localMatches = 0;
        for (PublicationWithLocation pub : batch) {
            Location pubLoc = pub.getLocation();
            Set<TreeNode> matchedSubscribers = new HashSet<>();
            for (SubscriptionWithRegion sub : subs) {
                // LOGIC: Deduplicates subscribers here (Correct)
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
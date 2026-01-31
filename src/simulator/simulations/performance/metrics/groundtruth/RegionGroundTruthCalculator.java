package simulator.simulations.performance.metrics.groundtruth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.Region; 
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class RegionGroundTruthCalculator implements GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(RegionGroundTruthCalculator.class.getName());

    // Threshold below which we just do it sequentially to avoid context switch overhead
    private static final long MIN_WORKLOAD_THRESHOLD = 1_000_000;

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs, ExecutorService executor) {
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

        boolean tooFewPubsForParallel = pubs.size() < numThreads && pubs.size() < 50;

        // If workload is small, use sequential logic (ignoring the passed executor)
        if (workload < MIN_WORKLOAD_THRESHOLD || numThreads <= 1 || tooFewPubsForParallel) {
            return calculateSequential(regionSubs, pubs);
        }

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
        }
        // Do NOT shutdown executor

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
        
        return total;
    }

    private long executeSubscriberParallel(ExecutorService executor,
                                           List<SubscriptionWithRegion> subs,
                                           List<PublicationWithLocation> pubs,
                                           int numThreads) throws InterruptedException, ExecutionException {
        
        logger.fine(String.format("--- Parallel Strategy B: Partitioning %d Subscribers across %d Threads ---", subs.size(), numThreads));

        long totalMatches = 0;
        int batchSize = (int) Math.ceil((double) subs.size() / numThreads);

        for (PublicationWithLocation pub : pubs) {
            List<Callable<Set<TreeNode>>> tasks = new ArrayList<>();
            Location pubLoc = pub.getLocation();
            
            for (int i = 0; i < subs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, subs.size());
                List<SubscriptionWithRegion> subBatch = subs.subList(i, end);

                tasks.add(() -> {
                    Set<TreeNode> localFound = new HashSet<>();
                    for (SubscriptionWithRegion s : subBatch) {
                        Region r = (Region) s.getRegion();
                        
                        if (r.contains(pubLoc)) {
                            if (s.getSource() != null) {
                                localFound.add(s.getSource());
                            }
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
        logger.finest("--- Calculating Region Ground Truth Matches (Sequential) ---");
        return countMatchesInBatch(pubs, subs);
    }

    private long countMatchesInBatch(List<PublicationWithLocation> batch, List<SubscriptionWithRegion> subs) {
        long localMatches = 0;
        for (PublicationWithLocation pub : batch) {
            Location pubLoc = pub.getLocation();

            Set<TreeNode> matchedSubscribers = new HashSet<>();
            for (SubscriptionWithRegion sub : subs) {

                Region r = (Region) sub.getRegion();
                
                if (r.contains(pubLoc)) {
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
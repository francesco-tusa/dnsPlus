package simulator.simulations.performance.metrics.groundtruth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import simulator.entities.SubscriberWithLocation;
import simulator.entities.context.SubscriberEvaluationContext;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import utils.CustomLogger;

public class ProximityGroundTruthCalculator implements GroundTruthCalculator {
    private static final Logger logger = CustomLogger.getLogger(ProximityGroundTruthCalculator.class.getName());

    @Override
    public long calculate(List<? extends SimulationSubscription> subs, List<PublicationWithLocation> pubs, ExecutorService executor) {
        if (subs.isEmpty() || pubs.isEmpty())
            return 0;

        List<SubscriptionWithLocation> locSubs = new ArrayList<>(subs.size());

        // 1. Filter and Cast
        for (SimulationSubscription s : subs) {
            if (s instanceof SubscriptionWithLocation swl) {
                locSubs.add(swl);
            }
        }

        if (locSubs.isEmpty())
            return 0;

        logger.fine("--- Calculating Proximity Ground Truth (Necessary Updates) ---");

        long totalNecessaryUpdates = 0;
        
        // Use the passed executor
        int numThreads = Runtime.getRuntime().availableProcessors(); 

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
                        
                        // ITERATE ALL PUBS
                        // Logic: Every time we find a publisher closer than our current 'best', 
                        // it counts as a 'Necessary Update' to improve the subscriber's state.
                        for (PublicationWithLocation pub : pubs) {
                            double d = sub.getLocation().distanceSquared(pub.getLocation());
                            if (d < bestDistSq) {
                                bestDistSq = d;
                                localCount++; // Increment on improvement
                            }
                        }

                        SubscriberWithLocation swl = (SubscriberWithLocation) sub.getSource();

                        if (swl != null) {
                            SubscriberEvaluationContext ctx = swl.getContext();
                            if (ctx != null) {
                                synchronized (swl) {
                                    // Update the subscriber's context with the Ideal (Proximity) distance
                                    if (bestDistSq < ctx.getGroundTruthMetric()) {
                                        ctx.setGroundTruthMetric(bestDistSq);
                                    }
                                }
                            }
                        } else {
                            // System.out.println("DEBUG: GT Calc - Source is NULL for sub " + sub.getId());
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
        } 

        logger.fine("--- Proximity Ground Truth Complete: " + totalNecessaryUpdates + " necessary updates. ---");
        return totalNecessaryUpdates;
    }
}
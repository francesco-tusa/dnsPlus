package simulator.workload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.WorkloadRepository;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import utils.CustomLogger;
import utils.SimulationRandom;

public class SubscriptionWorkloadOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(SubscriptionWorkloadOrchestrator.class.getName());
    private final Random random = SimulationRandom.get();
    
    // Batch size for streaming
    private static final int BATCH_SIZE_SUBSCRIBERS = 5000;
    // Log progress every 20 batches (approx every 100k subscribers)
    private static final int LOG_INTERVAL_BATCHES = 20;

    /**
     * Simplified entry point without metrics calculation.
     */
    public void generateAndDispatchWorkload(
            List<SubscriberWithLocation> subscribers, 
            List<BoundedBroker> leafBrokers, 
            SubscriptionWorkloadGenerator generator) {
        // Pass null for metrics and calculator
        generateDispatchAndCalculate(subscribers, leafBrokers, generator, null, null, null);
    }

    /**
     * Main orchestration method with support for Polymorphic Ground Truth calculation.
     */
    public void generateDispatchAndCalculate(
            List<SubscriberWithLocation> subscribers, 
            List<BoundedBroker> leafBrokers, 
            SubscriptionWorkloadGenerator generator,
            PerformanceMetricsData metricsData,
            List<PublicationWithLocation> allPublications,
            GroundTruthCalculator calculator) {
        
        WorkloadConfig config = SimConfiguration.get().workload;
        logger.info("Starting Streaming Workload Gen: Strategy=" + config.arrivalDistribution + 
                    ", Mean=" + config.meanSubscriptionsPerSubscriber + 
                    ", DensitySkew=" + config.enableDensitySkew);
        
        // 1. Shuffle subscribers to avoid spatial bias in batch processing
        List<SubscriberWithLocation> shuffledSubscribers = new ArrayList<>(subscribers);
        Collections.shuffle(shuffledSubscribers, SimulationRandom.get());

        int totalSubscribers = shuffledSubscribers.size();
        int batchSize = BATCH_SIZE_SUBSCRIBERS;
        WorkloadRepository repository = WorkloadRepository.getInstance();
        ExecutorService gtExecutor = Executors.newSingleThreadExecutor();

        logger.info("Processing workload in batches of approx " + batchSize + " subscribers.");

        int batchCounter = 0;

        try {
            for (int i = 0; i < totalSubscribers; i += batchSize) {
                int end = Math.min(i + batchSize, totalSubscribers);
                List<SubscriberWithLocation> batchSubscribers = shuffledSubscribers.subList(i, end);
                
                boolean shouldLog = (batchCounter % LOG_INTERVAL_BATCHES == 0) || (end == totalSubscribers);

                if (shouldLog) {
                    logger.info(String.format("--- Processing Batch %d (Subs %d-%d / %d) ---", batchCounter, i, end, totalSubscribers));
                }

                // A. Prepare Repository (Memory Allocation hint)
                int estimatedBatchVolume = calculateEstimatedVolume(config, batchSubscribers.size());
                repository.prepare(estimatedBatchVolume);

                // B. Generate (Populate Repository)
                generateBatchIntoRepository(batchSubscribers, leafBrokers, generator, config, repository);
                
                // C. Shuffle (Repository level shuffle to mix subscription order)
                repository.shuffle();
                
                // D. Async Ground Truth (Polymorphic Logic)
                Future<Long> gtFuture = null;
                // Only calculate if we have data, publications, AND a calculator strategy
                if (metricsData != null && allPublications != null && !allPublications.isEmpty() && calculator != null) {
                    
                    // Retrieve generated subscriptions. 
                    // Note: We use <? extends SimulationSubscription> to allow the calculator to cast 
                    // to SubscriptionWithRegion or SubscriptionWithLocation as needed.
                    List<? extends SimulationSubscription> batchSubsForGt = repository.getSubscriptions();
                    
                    gtFuture = gtExecutor.submit(() -> 
                        calculator.calculate(batchSubsForGt, allPublications)
                    );
                }

                // E. Dispatch (Send to Brokers)
                dispatch(repository, shouldLog);

                // F. Aggregate Results
                if (gtFuture != null) {
                    try {
                        long batchMatches = gtFuture.get();
                        metricsData.groundTruthMatches += batchMatches;
                        if (shouldLog) {
                            logger.info("Cumulative GT Matches (" + metricsData.getAlgorithmLabel() + "): " + metricsData.groundTruthMatches);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.severe("Error calculating Ground Truth for batch: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // G. Reset Repository for next batch
                WorkloadRepository.reset();
                batchCounter++;
            }
        } finally {
            gtExecutor.shutdown();
        }
        
        logger.info("Workload generation and dispatching complete.");
    }

    private void generateBatchIntoRepository(
            List<SubscriberWithLocation> batchSubscribers,
            List<BoundedBroker> leafBrokers,
            SubscriptionWorkloadGenerator generator,
            WorkloadConfig config,
            WorkloadRepository repository) {

        for (SubscriberWithLocation sub : batchSubscribers) {
            double lambda = config.meanSubscriptionsPerSubscriber;
            if (config.enableDensitySkew && sub.getBroker() instanceof BoundedBroker bb) {
                lambda = applyDensitySkew(lambda, bb);
            }

            int count = 0;
            switch (config.arrivalDistribution) {
                case UNIFORM -> {
                    count = (int) Math.round(lambda);
                    if (count == 0 && lambda > 0) count = 1; 
                }
                case POISSON -> {
                    count = getPoisson(lambda);
                    if (count == 0 && config.meanSubscriptionsPerSubscriber >= 1.0) count = 1;
                }
            }

            if (count > 0) {
                List<SimulationSubscription> subs = generator.generateSubscriptionBatch(sub, leafBrokers, count);
                for (SimulationSubscription s : subs) {
                    s.setSource(sub); 
                    repository.add(s);
                }
            }
        }
    }
    
    private int calculateEstimatedVolume(WorkloadConfig config, int subscriberCount) {
        double multiplier = config.meanSubscriptionsPerSubscriber;
        if (config.enableDensitySkew) multiplier *= 1.5; 
        return (int) (subscriberCount * Math.max(1.0, multiplier) * 1.1);
    }

    private double applyDensitySkew(double baseLambda, BoundedBroker broker) {
        long pop = broker.getInternetPopulation();
        if (pop > 1_000_000) return baseLambda * 2.0;
        if (pop > 500_000)   return baseLambda * 1.5;
        return baseLambda;
    }

    private void dispatch(WorkloadRepository repository, boolean verbose) {
        List<SimulationSubscription> allSubs = repository.getSubscriptions();
        int size = allSubs.size();
        
        if (verbose) {
            logger.info("Dispatching batch of " + size + " subscriptions...");
        }
        
        for (int i = 0; i < size; i++) {
            SimulationSubscription s = allSubs.get(i);
            if (s.getSource() instanceof SubscriberWithLocation sub) {
                sub.send(s);
            }
        }
    }

    private int getPoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);
        return k - 1;
    }
}
package simulator.workload;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.WorkloadRepository;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import utils.CustomLogger;

public class SubscriptionWorkloadOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(SubscriptionWorkloadOrchestrator.class.getName());
    private final Random random = new Random();

    /**
     * Generates workload, populates the WorkloadRepository, and dispatches events.
     * Returns void because the data is stored centrally in the Repository.
     */
    public void generateAndDispatchWorkload(
            List<SubscriberWithLocation> subscribers, 
            List<BoundedBroker> leafBrokers, 
            SubscriptionWorkloadGenerator generator) {
        
        WorkloadConfig config = SimConfiguration.get().workload;
        logger.info("Starting Workload Gen: Strategy=" + config.arrivalDistribution + 
                    ", Mean=" + config.meanSubscriptionsPerSubscriber + 
                    ", DensitySkew=" + config.enableDensitySkew);
        
        // 1. Dynamic Size Calculation & Pre-allocation
        int estimatedSize = calculateEstimatedVolume(config, subscribers.size());
        logger.info("Pre-allocating Repository for approx. " + estimatedSize + " subscriptions.");
        
        WorkloadRepository repository = WorkloadRepository.getInstance();
        repository.prepare(estimatedSize);

        int totalGenerated = 0;

        // 2. Generation Phase
        for (SubscriberWithLocation sub : subscribers) {
            
            double lambda = config.meanSubscriptionsPerSubscriber;
            
            // Density Skew Logic
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
                    // Link source and store immediately
                    s.setSource(sub); 
                    repository.add(s);
                }
                totalGenerated += subs.size();
            }
        }

        logger.info(String.format("Generated %d subscriptions for %d subscribers.", totalGenerated, subscribers.size()));

        // 3. Shuffle (In-Place)
        repository.shuffle();

        // 4. Dispatch Phase
        dispatch(repository);
    }
    
    private int calculateEstimatedVolume(WorkloadConfig config, int subscriberCount) {
        double multiplier = config.meanSubscriptionsPerSubscriber;
        // If skew is enabled, add a buffer (e.g., 50% overhead safety margin)
        if (config.enableDensitySkew) {
            multiplier *= 1.5; 
        }
        // Add 10% general buffer for Poisson variance
        return (int) (subscriberCount * Math.max(1.0, multiplier) * 1.1);
    }

    private double applyDensitySkew(double baseLambda, BoundedBroker broker) {
        long pop = broker.getInternetPopulation();
        if (pop > 1_000_000) return baseLambda * 2.0;
        if (pop > 500_000)   return baseLambda * 1.5;
        return baseLambda;
    }

    private void dispatch(WorkloadRepository repository) {
        List<SimulationSubscription> allSubs = repository.getSubscriptions();
        int size = allSubs.size();
        logger.info("Dispatching " + size + " subscriptions...");
        
        int interval = Math.max(1000, size / 10);
        
        for (int i = 0; i < size; i++) {
            SimulationSubscription s = allSubs.get(i);
            
            // The source is already embedded in the event
            if (s.getSource() instanceof SubscriberWithLocation sub) {
                sub.send(s);
            }
            
            if ((i + 1) % interval == 0) {
                logger.info(String.format("  ... dispatched %d / %d", (i+1), size));
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
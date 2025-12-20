package simulator.workload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import utils.CustomLogger;

public class SubscriptionWorkloadOrchestrator {

    private static final Logger logger = CustomLogger.getLogger(SubscriptionWorkloadOrchestrator.class.getName());
    private final Random random = new Random();

    public List<SimulationSubscription> generateAndDispatchWorkload(
            List<SubscriberWithLocation> subscribers, 
            List<BoundedBroker> leafBrokers, 
            SubscriptionWorkloadGenerator generator) {
        
        WorkloadConfig config = SimConfiguration.get().workload;
        logger.info("Starting Workload Gen: Strategy=" + config.arrivalDistribution + 
                    ", Mean=" + config.meanSubscriptionsPerSubscriber + 
                    ", DensitySkew=" + config.enableDensitySkew);
        
        List<PendingSubscription> globalQueue = new ArrayList<>();
        List<SimulationSubscription> allGeneratedSubscriptions = new ArrayList<>();
        int totalGenerated = 0;

        for (SubscriberWithLocation sub : subscribers) {
            
            // 1. Calculate Target Mean for this specific user
            double lambda = config.meanSubscriptionsPerSubscriber;
            
            // [NEW] External Factor: Density Skew
            if (config.enableDensitySkew && sub.getBroker() instanceof BoundedBroker bb) {
                lambda = applyDensitySkew(lambda, bb);
            }

            // 2. Determine Count based on Distribution
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

            // 3. Generate Content
            if (count > 0) {
                List<SimulationSubscription> subs = generator.generateSubscriptionBatch(sub, leafBrokers, count);
                for (SimulationSubscription s : subs) {
                    globalQueue.add(new PendingSubscription(sub, s));
                    allGeneratedSubscriptions.add(s);
                }
                totalGenerated += subs.size();
            }
        }

        logger.info(String.format("Generated %d subscriptions for %d subscribers.", totalGenerated, subscribers.size()));

        // 4. Shuffle & Dispatch
        Collections.shuffle(globalQueue, random);
        dispatch(globalQueue);
        
        return allGeneratedSubscriptions;
    }

    /**
     * [External Factor Logic]
     * Increases lambda for brokers with huge populations (simulating "active city users").
     */
    private double applyDensitySkew(double baseLambda, BoundedBroker broker) {
        long pop = broker.getInternetPopulation();
        if (pop > 1_000_000) return baseLambda * 2.0;
        if (pop > 500_000)   return baseLambda * 1.5;
        return baseLambda;
    }

    private void dispatch(List<PendingSubscription> queue) {
        logger.info("Dispatching " + queue.size() + " subscriptions...");
        int interval = Math.max(1000, queue.size() / 10);
        for (int i = 0; i < queue.size(); i++) {
            PendingSubscription event = queue.get(i);
            event.subscriber.send(event.subscription);
            if ((i + 1) % interval == 0) logger.info(String.format("  ... dispatched %d / %d", (i+1), queue.size()));
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

    private static class PendingSubscription {
        final SubscriberWithLocation subscriber;
        final SimulationSubscription subscription;
        PendingSubscription(SubscriberWithLocation sub, SimulationSubscription s) {
            this.subscriber = sub;
            this.subscription = s;
        }
    }
}
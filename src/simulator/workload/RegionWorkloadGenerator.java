package simulator.workload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;
import utils.SimulationRandom;

public class RegionWorkloadGenerator implements SubscriptionWorkloadGenerator {

    private static final Logger logger = CustomLogger.getLogger(RegionWorkloadGenerator.class.getName());
    private final Random random = SimulationRandom.get();
    
    private List<BoundedBroker> hotspots = null;
    private double hotspotInterestProbability = 0.8; 

    @Override
    public void setHotspots(List<BoundedBroker> hotspots) {
        this.hotspots = hotspots;
    }

    public void setHotspotInterestProbability(double prob) {
        this.hotspotInterestProbability = prob;
    }

    @Override
    public SimulationSubscription generateSubscription(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers) {
        double remoteProb = SimConfiguration.get().workload.remoteInterestProbability;
        double regionSize = SimConfiguration.get().workload.subscriptionRegionSize;
        double jitter = SimConfiguration.get().workload.locationJitter; // [NEW] Read Config
        
        Region subscriptionRegion;
        
        if (random.nextDouble() < remoteProb) {
            // --- Remote Interest (No Jitter, typically static hotspots) ---
            if (hotspots != null && !hotspots.isEmpty() && random.nextDouble() < hotspotInterestProbability) {
                BoundedBroker targetHub = hotspots.get(random.nextInt(hotspots.size()));
                subscriptionRegion = new Region(targetHub.getRegion());
            } else {
                BoundedBroker randomBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));
                subscriptionRegion = new Region(randomBroker.getRegion());
            }
        } else {
            // --- Local Interest with Jitter ---
            Location center = subscriber.getLocation();
            
            // Apply Gaussian Noise if jitter > 0
            double shiftX = 0.0;
            double shiftY = 0.0;
            if (jitter > 0) {
                shiftX = random.nextGaussian() * jitter;
                shiftY = random.nextGaussian() * jitter;
            }

            double centerX = center.getX() + shiftX;
            double centerY = center.getY() + shiftY;
            double halfSize = regionSize / 2.0; 

            subscriptionRegion = new Region(
                new Location(centerX - halfSize, centerY - halfSize, 0),
                new Location(centerX + halfSize, centerY + halfSize, 0)
            );
        }
        
        return new SubscriptionWithRegion(subscriptionRegion);
    }

    @Override
    public List<SimulationSubscription> generateSubscriptionBatch(SubscriberWithLocation subscriber, List<BoundedBroker> leafBrokers, int count) {
        List<SimulationSubscription> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(generateSubscription(subscriber, leafBrokers));
        }
        return batch;
    }
}
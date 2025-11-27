package simulator.workload;

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

public class RegionWorkloadGenerator implements SubscriptionWorkloadGenerator {

    private static final Logger logger = CustomLogger.getLogger(RegionWorkloadGenerator.class.getName());
    private final Random random = new Random();
    
    // Hotspots (e.g. AWS Regions or Top Cities) injected by the simulation runner
    private List<BoundedBroker> hotspots = null;
    
    // Probability that a REMOTE request targets a Hotspot vs a Random Niche location
    // Default 0.8 means 80% of remote traffic goes to AWS/Big Cities, 20% goes to random locations.
    private double hotspotInterestProbability = 0.8; 

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

        Region subscriptionRegion;
        
        // 1. Remote Interest?
        if (random.nextDouble() < remoteProb) {
            
            // 1A. Hotspot Interest? (The "Netflix" case)
            if (hotspots != null && !hotspots.isEmpty() && random.nextDouble() < hotspotInterestProbability) {
                BoundedBroker targetHub = hotspots.get(random.nextInt(hotspots.size()));
                subscriptionRegion = new Region(targetHub.getRegion());
            } 
            
            // 1B. Niche/Random Interest? (The "Siberian Sensor" case)
            else {
                // Pick ANY leaf broker in the world randomly
                BoundedBroker randomBroker = leafBrokers.get(random.nextInt(leafBrokers.size()));
                subscriptionRegion = new Region(randomBroker.getRegion());
            }
            
        } 
        // 2. Local Interest (Centered on Subscriber)
        else {
            Location center = subscriber.getLocation();
            double halfSize = regionSize / 2.0; 
            subscriptionRegion = new Region(
                new Location(center.getX() - halfSize, center.getY() - halfSize, 0),
                new Location(center.getX() + halfSize, center.getY() + halfSize, 0)
            );
        }
        
        return new SubscriptionWithRegion(subscriptionRegion);
    }
}
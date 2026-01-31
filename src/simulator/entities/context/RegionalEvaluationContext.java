package simulator.entities.context;

import simulator.config.SimConfiguration;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CsvMetricWriter;

public class RegionalEvaluationContext implements SubscriberEvaluationContext {
    
    private float[] regionCoords = null;
    private int activeRegionCount = 0;
    
    @Override
    public void onSubscriptionSent(SimulationSubscription s) {
        if (s instanceof SubscriptionWithRegion swr) {
            ensureCapacity();
            int offset = activeRegionCount * 4;
            regionCoords[offset]   = swr.getMinLon();
            regionCoords[offset+1] = swr.getMaxLon();
            regionCoords[offset+2] = swr.getMinLat();
            regionCoords[offset+3] = swr.getMaxLat();
            activeRegionCount++;
        }
    }

    @Override
    public void onPublicationReceived(SubscriberWithLocation sub, PublicationWithLocation pub) {
        boolean matchesInterest = checkRegionInterest(pub);
        if (!matchesInterest) {
            sub.incrementFalsePositiveDeliveries(); 
        }
        
        // Trigger trace with the result of the interest check
        if (SimConfiguration.get().paths.enableEventTracing) {
            tracePublication(sub, pub, matchesInterest);
        }
    }

    @Override
    public void traceSubscription(SubscriberWithLocation sub, SimulationSubscription s, SimulationBroker broker) {
        // Standard logging for Regional: just "SENT"
        CsvMetricWriter.getInstance().logSubscriberEvent(
            s, 
            broker.getName(), 
            sub.getLon(), 
            sub.getLat(), 
            "SENT"
        );
    }

    @Override
    public void tracePublication(SubscriberWithLocation sub, PublicationWithLocation pub, boolean matchesInterest) {
        if (pub.getMetrics() == null) return;

        String result = matchesInterest ? "Delivered" : "FalsePositive";
            
        CsvMetricWriter.getInstance().logSubscriberPublicationEvent(
            pub,
            sub.getName(),
            result,
            sub.getLon(),
            sub.getLat(),
            -1.0 // No distance context in Regional
        );
    }

    private boolean checkRegionInterest(PublicationWithLocation pub) {
        if (activeRegionCount == 0) return true; 
        if (regionCoords == null) return false;
        
        float pubLon = (float) pub.getLocation().getX();
        float pubLat = (float) pub.getLocation().getY();
        
        for (int i = 0; i < activeRegionCount; i++) {
            int offset = i * 4;
            if (Region.fastContains(
                    regionCoords[offset], regionCoords[offset+1], 
                    regionCoords[offset+2], regionCoords[offset+3], 
                    pubLon, pubLat)) {
                return true;
            }
        }
        return false;
    }

    private void ensureCapacity() {
        if (regionCoords == null) {
            regionCoords = new float[4];
        } else if (activeRegionCount * 4 >= regionCoords.length) {
            float[] newArr = new float[regionCoords.length + 16];
            System.arraycopy(regionCoords, 0, newArr, 0, regionCoords.length);
            regionCoords = newArr;
        }
    }
}
package simulator.entities.context;

import simulator.config.SimConfiguration;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import utils.CsvMetricWriter;

public class ProximityEvaluationContext implements SubscriberEvaluationContext {
    private double groundTruthMinDistSq = Double.MAX_VALUE;
    
    // This tracks the BEST distance received SO FAR. 
    // It is used for both the "Stretch" metric AND the "Update/NoUpdate" trace logic.
    private double receivedMinDistSq = Double.MAX_VALUE;

    @Override
    public void setGroundTruthMetric(double distSq) {
        this.groundTruthMinDistSq = distSq;
    }

    @Override
    public double getGroundTruthMetric() {
        return groundTruthMinDistSq;
    }

    @Override
    public double getStretchMetric() {
        return receivedMinDistSq;
    }

    @Override
    public void onSubscriptionSent(SimulationSubscription s) {
        // No region tracking needed
    }

    @Override
    public void onPublicationReceived(SubscriberWithLocation sub, PublicationWithLocation pub) {
        double distSq = pub.getCachedDistanceSquared();
        if (distSq < 0) {
             distSq = sub.getLocation().distanceSquared(pub.getLocation());
        }
        
        boolean isImprovement = false;
        synchronized(this) {
            if (distSq < receivedMinDistSq) {
                receivedMinDistSq = distSq;
                isImprovement = true;
            }
        }

        if (SimConfiguration.get().paths.enableEventTracing) {
            // We pass 'isImprovement' as the "interesting" flag for the trace logic
            tracePublication(sub, pub, isImprovement);
        }
    }

    @Override
    public void traceSubscription(SubscriberWithLocation sub, SimulationSubscription s, SimulationBroker broker) {
         CsvMetricWriter.getInstance().logSubscriberEvent(
            s, 
            broker.getName(), 
            sub.getLon(), 
            sub.getLat(), 
            "SENT"
        );
    }

    @Override
    public void tracePublication(SubscriberWithLocation sub, PublicationWithLocation pub, boolean isImprovement) {
        if (pub.getMetrics() == null) return;
        
        // Retrieve the distance for logging
        double currentDistSq = pub.getCachedDistanceSquared();
        if (currentDistSq < 0) {
            currentDistSq = sub.getLocation().distanceSquared(pub.getLocation());
        }

        String status;
        // In Proximity, "matchesInterest" (isImprovement) means it updated our best-so-far
        if (receivedMinDistSq == Double.MAX_VALUE || (isImprovement && receivedMinDistSq == currentDistSq)) {
            // If it's the first one, or it matches the current best (which we just updated)
            status = (receivedMinDistSq == currentDistSq && receivedMinDistSq == currentDistSq) ? "NEW" : "UPDATE"; 
            // Logic fix: simplistic approach below
        } else {
             status = "NO_UPDATE";
        }
        
        // Refined Logic based on previous state:
        // Since we update state BEFORE trace, 'receivedMinDistSq' is already the new value.
        // If currentDistSq == receivedMinDistSq, it was an update.
        // However, distinguishing "First Ever" (NEW) from "Better" (UPDATE) is tricky without extra state,
        // but generally "UPDATE" covers both valid cases in Proximity. 
        // Let's refine based on the flag passed in.
        
        if (isImprovement) {
            status = "UPDATE"; 
        } else {
            status = "NO_UPDATE";
        }

        CsvMetricWriter.getInstance().logSubscriberPublicationEvent(
            pub,
            sub.getName(),
            status,
            sub.getLon(),
            sub.getLat(),
            currentDistSq
        );
    }
}
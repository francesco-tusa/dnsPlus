package simulator.entities.context;

import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;

public interface SubscriberEvaluationContext {
    
    // --- Logic Hooks ---
    void onSubscriptionSent(SimulationSubscription s);
    void onPublicationReceived(SubscriberWithLocation sub, PublicationWithLocation p);
    
    // --- Trace Hooks ---
    /**
     * Handles the logging of subscription events (e.g., SENT).
     */
    void traceSubscription(SubscriberWithLocation sub, SimulationSubscription s, SimulationBroker broker);

    /**
     * Handles the logging of publication events (e.g., RECEIVED, UPDATE, FALSE_POSITIVE).
     * @param matchesInterest True if the publication was valid/interesting for this subscriber
     */
    void tracePublication(SubscriberWithLocation sub, PublicationWithLocation p, boolean matchesInterest);

    // --- Metric Getters ---
    default double getStretchMetric() { return 0.0; }
    default double getGroundTruthMetric() { return 0.0; }
    default void setGroundTruthMetric(double value) {}
}
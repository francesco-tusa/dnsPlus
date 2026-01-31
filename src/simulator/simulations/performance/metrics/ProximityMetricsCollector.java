package simulator.simulations.performance.metrics;

import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.ProximityRoutingBroker;

public class ProximityMetricsCollector extends MetricsCollector {

    @Override
    protected void visitBroker(SimulationBroker b, PerformanceMetricsData data) {
        super.visitBroker(b, data); // Collect common stats first

        // Collect Proximity-specific broker stats (Brake mechanism)
        if (b instanceof ProximityRoutingBroker pb && data instanceof ProximityPerformanceMetricsData pd) {
            pd.totalBrakeSuppressedEvents += pb.getBrakeFilteredCount();
            pd.totalUpstreamSubscriptionUpdates += pb.getOutSubAddedCount();
        }
    }

    @Override
    protected void visitSubscriber(SubscriberWithLocation s, PerformanceMetricsData data) {
        super.visitSubscriber(s, data); // Collect common stats first

        // Collect Stretch Metrics
        if (data instanceof ProximityPerformanceMetricsData pd) {
            double actualSq = s.getContext().getStretchMetric();
            double idealSq = s.getContext().getGroundTruthMetric();

            if (actualSq < Double.MAX_VALUE && idealSq < Double.MAX_VALUE) {
                double actualDist = Math.sqrt(actualSq);
                double idealDist = Math.sqrt(idealSq);
                double stretch = actualDist - idealDist;
                
                if (stretch < 0) stretch = 0.0;
                
                pd.stretchStats.accept(stretch);
            }
        }
    }
}
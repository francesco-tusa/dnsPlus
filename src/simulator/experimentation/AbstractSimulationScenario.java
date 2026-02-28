package simulator.experimentation;

import simulator.simulations.performance.metrics.PerformanceMetricsData;
import java.util.Properties;

public abstract class AbstractSimulationScenario implements SimulationScenario {

    protected String currentExperimentId = "unknown";

    @Override
    public void configure(Properties props) {
        configureSpecific(props);
    }

    protected abstract void configureSpecific(Properties props);

    protected String getCommonCsvHeader() {
        return "experiment_id,publishers,subscribers";
    }

    protected String getCommonCsvPrefix(Properties config) {
        String publishers = config.getProperty("workload.replicas");
        if (publishers == null) {
            publishers = config.getProperty("workload.publishers.count", "Unknown");
        }

        String subscribers = config.getProperty("workload.subscribers.count", "Unknown");

        return String.format("%s,%s,%s", this.currentExperimentId, publishers, subscribers);
    }

    // --- Common Formatting & Calculation Helpers ---

    protected String formatDouble(double value) {
        return String.format("%.4f", value);
    }

    protected double safeDiv(long numerator, long denominator) {
        return (denominator > 0) ? (double) numerator / denominator : 0.0;
    }

    protected double safeDiv(double numerator, double denominator) {
        return (denominator > 0) ? numerator / denominator : 0.0;
    }

    protected double calculateTrafficRatio(PerformanceMetricsData data) {
        return safeDiv(data.totalPublicationProcessingEvents, data.totalDeliveriesReceived);
    }
}
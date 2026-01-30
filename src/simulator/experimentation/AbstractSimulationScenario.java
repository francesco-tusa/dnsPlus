package simulator.experimentation;

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

    /**
     * Helper to generate the first 3 columns.
     * Uses the local currentExperimentId field.
     */
    protected String getCommonCsvPrefix(Properties config) {
        String publishers = config.getProperty("workload.replicas");
        if (publishers == null) {
            publishers = config.getProperty("workload.publishers.count", "Unknown");
        }

        String subscribers = config.getProperty("workload.subscribers.count", "Unknown");

        return String.format("%s,%s,%s", this.currentExperimentId, publishers, subscribers);
    }
}
package simulator.experimentation;

import java.util.Properties;
import simulator.simulations.performance.metrics.PerformanceMetricsData;

public interface SimulationScenario {
    /**
     * @return The specific simulation property to vary (e.g., "region.fpr.threshold").
     */
    String getKnobKey();

    /**
     * @return The list of values to test for this knob.
     * Use String to accommodate both Integers (Brake) and Doubles (FPR).
     */
    String[] getKnobValues();

    /**
     * @return The CSV header corresponding to the specific metrics of this scenario.
     */
    String getCsvHeader();

    /**
     * Converts the raw metrics and config into a CSV line.
     */
    String getCsvRow(PerformanceMetricsData metrics, Properties currentConfig);

    /**
     * Instantiates and runs the specific simulation logic.
     */
    PerformanceMetricsData runSimulation();

    /**
     * Allows the scenario to inject specific configuration properties 
     * (e.g., Brake Strategy, specific Bounds) before the run starts.
     */
    void configure(Properties props);
}
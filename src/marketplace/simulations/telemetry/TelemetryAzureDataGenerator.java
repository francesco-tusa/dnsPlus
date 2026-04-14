package marketplace.simulations.telemetry;

import java.io.File;
import marketplace.config.MarketplaceConfig;
import marketplace.simulations.AzureMarketplaceSimulation;
import marketplace.optimization.TelemetryLoggingStrategy;

public class TelemetryAzureDataGenerator extends AzureMarketplaceSimulation {

    public TelemetryAzureDataGenerator() {
        super();
    }

    @Override
    protected void setupSimulation() {
        super.setupSimulation();

        if (MarketplaceConfig.get().collectFlTelemetry) {
            // Retrieve the path dictated by the FLDatasetSweepScenario
            String basePath = System.getProperty("marketplace.ml.telemetry_base_path");
            
            // Append the unique run ID to prevent overwrite during statistical repetitions
            String path = (basePath != null && !basePath.isEmpty()) ? 
                          basePath + "_Run_" + this.getSimulationId() : 
                          "output" + File.separator + this.getSimulationId() + File.separator + "telemetry_dump";
            
            TelemetryLoggingStrategy.setDumpDirectory(path);
            logger.info("RL Telemetry dump directory bound to: " + path);
        }
    }

    @Override
    protected void collectAndPrintMetrics() {
        super.collectAndPrintMetrics();

        if (MarketplaceConfig.get().collectFlTelemetry) {
            logger.info("--- Finalizing FaaS Marketplace HFL Data Generation ---");
            TelemetryLoggingStrategy.flushAndCloseAll();
        }
    }
}
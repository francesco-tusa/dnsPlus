package marketplace.population.tiers;

import java.util.Random;

public interface ProviderTierStrategy {
    double getCoverageRadius();
    double generateBaseComputeLatency(Random r);
    double generateBaseBandwidth(Random r);
    double getBaseCost();
    double getBaseReliability();

    // --- Polymorphic Hardware Physics ---
    
    /**
     * @return The baseline latency (ms) required for the tier's orchestrator 
     * to provision a new container/microVM namespace.
     */
    double getControlPlaneProvisioningMs();

    /**
     * @return The scalar multiplier representing the tier's memory bus and CPU speed 
     * when moving state into RAM. Lower is faster.
     */
    double getMemorySpeedMultiplier();
}
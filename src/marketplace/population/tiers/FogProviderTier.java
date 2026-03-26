package marketplace.population.tiers;
import java.util.Random;

public class FogProviderTier implements ProviderTierStrategy {
    @Override public double getCoverageRadius() { return 3.0; } // ~330km
    @Override public double generateBaseComputeLatency(Random r) { return 10.0 + (r.nextDouble() * 5.0); }
    @Override public double generateBaseBandwidth(Random r) { return 200.0 + (r.nextDouble() * 300.0); }
    @Override public double getBaseCost() { return 6.00; } // Regional premium
    @Override public double getBaseReliability() { return 0.99; }


    // Fog: Medium orchestration, standard virtualized memory bus
    @Override public double getControlPlaneProvisioningMs() { return 200.0; }
    @Override public double getMemorySpeedMultiplier() { return 2.0; }
}
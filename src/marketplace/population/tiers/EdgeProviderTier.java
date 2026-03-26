package marketplace.population.tiers;
import java.util.Random;

public class EdgeProviderTier implements ProviderTierStrategy {
    @Override public double getCoverageRadius() { return 0.25; } // ~27km Metro
    @Override public double generateBaseComputeLatency(Random r) { return 15.0 + (r.nextDouble() * 10.0); }
    @Override public double generateBaseBandwidth(Random r) { return 20.0 + (r.nextDouble() * 80.0); }
    @Override public double getBaseCost() { return 12.50; } // High perimeter premium
    @Override public double getBaseReliability() { return 0.95; }

    // Edge: Fast local orchestration (e.g., K3s/containerd), but weaker CPU/Memory bus
    @Override public double getControlPlaneProvisioningMs() { return 50.0; }
    @Override public double getMemorySpeedMultiplier() { return 3.5; }
}
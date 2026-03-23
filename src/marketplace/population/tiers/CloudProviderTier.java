package marketplace.population.tiers;
import java.util.Random;

public class CloudProviderTier implements ProviderTierStrategy {
    @Override public double getCoverageRadius() { return 8.0; } // ~900km 
    @Override public double generateBaseComputeLatency(Random r) { return 2.0 + (r.nextDouble() * 5.0); }
    @Override public double generateBaseBandwidth(Random r) { return 800.0 + (r.nextDouble() * 200.0); }
    @Override public double getBaseCost() { return 2.50; } // Base AWS Lambda equivalent
    @Override public double getBaseReliability() { return 0.999; }
}
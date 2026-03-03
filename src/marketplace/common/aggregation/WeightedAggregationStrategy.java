package marketplace.common.aggregation;

public class WeightedAggregationStrategy implements AggregationStrategy {
    @Override
    public double calculateAggregatedValue(double valueA, double weightA, double valueB, double weightB) {
        double totalWeight = weightA + weightB;
        if (totalWeight == 0) return (valueA + valueB) / 2.0; // Fallback for safety
        
        return ((valueA * weightA) + (valueB * weightB)) / totalWeight;
    }
}
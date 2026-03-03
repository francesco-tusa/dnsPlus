package marketplace.common.aggregation;

public class UnweightedAggregationStrategy implements AggregationStrategy {
    @Override
    public double calculateAggregatedValue(double valueA, double weightA, double valueB, double weightB) {
        // Completely ignores the weights (population size) and returns the geometric midpoint
        return (valueA + valueB) / 2.0;
    }
}
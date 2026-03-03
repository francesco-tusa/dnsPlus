package marketplace.common.aggregation;

public interface AggregationStrategy {
    /**
     * Calculates the aggregated mean/midpoint of two values based on the underlying strategy.
     */
    double calculateAggregatedValue(double valueA, double weightA, double valueB, double weightB);
}
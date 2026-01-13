package simulator.simulations.performance;

import simulator.core.TreeNode;
import simulator.regions.ProximityRoutingBroker;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.ProximityPerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import utils.CsvMetricWriter.TraceMetricStrategy;
import utils.CsvMetricWriter.RegionTraceStrategy;
import utils.CsvMetricWriter.ProximityTraceStrategy;

public enum SimulationType {
    REGION {
        @Override
        public PerformanceMetricsData createMetricsData() {
            return new RegionPerformanceMetricsData();
        }

        @Override
        public TraceMetricStrategy createTraceStrategy() {
            return new RegionTraceStrategy();
        }
    },
    PROXIMITY {
        @Override
        public PerformanceMetricsData createMetricsData() {
            return new ProximityPerformanceMetricsData();
        }

        @Override
        public TraceMetricStrategy createTraceStrategy() {
            return new ProximityTraceStrategy();
        }
    };

    /**
     * Infers the simulation type based on the root node of the topology.
     */
    public static SimulationType infer(TreeNode root) {
        if (root instanceof ProximityRoutingBroker) {
            return PROXIMITY;
        }
        return REGION; // Default fallback for SpatialMatchBroker and others
    }
    
    public abstract PerformanceMetricsData createMetricsData();
    public abstract TraceMetricStrategy createTraceStrategy();
}
package simulator.simulations.performance.metrics;

import simulator.entities.SimulationBroker;
import simulator.regions.BoundedBroker;

public class RegionMetricsCollector extends MetricsCollector {

    @Override
    protected void visitBroker(SimulationBroker b, PerformanceMetricsData data) {
        super.visitBroker(b, data); // Collect common stats first

        // Collect Region-specific broker stats
        if (b instanceof BoundedBroker bb && data instanceof RegionPerformanceMetricsData rd) {
            rd.totalInputCovered += bb.getSubCoveredCount();
            rd.totalInputExpanded += bb.getSubExpandedCount();
            rd.totalInputSimpleExpanded += bb.getSubSimpleExpandedCount();
            rd.totalInputComplexExpanded += bb.getSubComplexExpandedCount();
            rd.totalInputAdded += bb.getSubAddedCount();
            rd.totalInputMerged += bb.getSubMergedCount();
            rd.totalInputAbsorbed += bb.getSubAbsorbedCount();
            
            rd.totalPropagatedCovered += bb.getOutSubCoveredCount();
            rd.totalPropagatedExpanded += bb.getOutSubExpandedCount();
            rd.totalPropagatedSimpleExpanded += bb.getOutSubSimpleExpandedCount();
            rd.totalPropagatedComplexExpanded += bb.getOutSubComplexExpandedCount();
            rd.totalPropagatedAdded += bb.getOutSubAddedCount();
            rd.totalPropagatedMerged += bb.getOutSubMergedCount();
            rd.totalPropagatedAbsorbed += bb.getOutSubAbsorbedCount();
        }
    }
}
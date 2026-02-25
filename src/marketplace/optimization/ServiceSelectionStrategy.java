package marketplace.optimization;

import java.util.List;
import java.util.Map;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import marketplace.events.ServiceRequest;

public interface ServiceSelectionStrategy {
    record SelectionResult(TreeNode bestNode, double bestScore, double distance, String diagnosis) {}

    SelectionResult selectBestProvider(ServiceRequest request, Map<TreeNode, List<SimulationSubscription>> candidates);
}
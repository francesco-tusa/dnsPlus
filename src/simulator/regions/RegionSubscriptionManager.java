package simulator.regions;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

public class RegionSubscriptionManager {

    private static final Logger logger = CustomLogger.getLogger(RegionSubscriptionManager.class.getName());
    private final Map<TreeNode, SimulationSubscription> stateMap;

    public RegionSubscriptionManager(Map<TreeNode, SimulationSubscription> map) {
        this.stateMap = map;
    }

    public boolean updateOrExpand(TreeNode target, SubscriptionWithRegion newSub) {
        SimulationSubscription existing = stateMap.get(target);

        if (existing == null) {
            stateMap.put(target, newSub);
            // Changed to FINE to avoid polluting console during performance runs
            logger.fine("Manager (" + target.getName() + "): New Entry Added. Region: " + newSub.getRegion().toLogString());
            return true; 
        }

        if (existing instanceof SubscriptionWithRegion existingSub) {
            Region currentRegion = existingSub.getRegion();
            Region newRegion = newSub.getRegion();

            if (currentRegion.contains(newRegion)) {
                // Changed to FINE. This is normal behavior in large simulations and shouldn't be INFO.
                logger.fine("Manager (" + target.getName() + "): REDUNDANT. Current: " + currentRegion.toLogString() + " contains Incoming: " + newRegion.toLogString());
                return false; 
            } else {
                // Capture state BEFORE mutation for logging
                String oldState = currentRegion.toLogString();
                
                boolean expanded = currentRegion.expand(newRegion);
                
                // Changed to FINE.
                if (expanded) {
                    logger.fine("Manager (" + target.getName() + "): EXPANDED. Old: " + oldState + 
                                " Incoming: " + newRegion.toLogString() + 
                                " Result: " + currentRegion.toLogString());
                } else {
                     logger.fine("Manager (" + target.getName() + "): EXPAND called but no change detected.");
                }
                return expanded; 
            }
        }
        
        stateMap.put(target, newSub);
        return true;
    }
}
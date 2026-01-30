package simulator.regions;

import java.util.logging.Logger;
import utils.CustomLogger;

import simulator.core.Location;
import simulator.core.TreeNode;


/**
 * Leaf node implementation for the "Heavy version, closest" algorithm.
 * * Logic Alignment:
 * 1. Inherits the Stateful Routing logic from ProximityRoutingBroker.
 * 2. Subscribers are treated as "Point Targets" (Target Array Size = 1).
 * 3. Maintains 'Best Distance' state per subscriber to filter redundant
 * updates.
 * 4. Aggregates all subscribers into the Broker's Center for upstream
 * propagation.
 */
public class ProximityRoutingLeafBroker extends ProximityRoutingBroker implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(ProximityRoutingLeafBroker.class.getName());

    public ProximityRoutingLeafBroker(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    public ProximityRoutingLeafBroker(String name) {
        super(name);
    }

    /**
     * strict enforcement of Leaf semantics (optional but recommended):
     * Leaf brokers should generally only have Subscribers as children, not other
     * Brokers.
     */
    @Override
    public void addChild(TreeNode child) {
        // Validation: Warn if a Broker is added as a child to a Leaf
        if (child instanceof BoundedBroker) {
            logger.warning("Topology Warning: Adding a Broker (" + child.toString() +
                    ") as a child to a LeafBroker (" + this.getName() + "). " +
                    "Leafs usually only host Subscribers.");
        }

        // Use the parent's logic to register the topology targets (Points/Quadrants)
        super.addChild(child);
    }
}
package marketplace.tests.fixtures;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.tests.framework.TopologyFixture;
import simulator.topology.factories.BrokerFactory;
import simulator.topology.factories.BoundedBrokerFactory;
import marketplace.agents.MarketplaceBroker;

/**
 * A custom topology fixture that establishes a Cloud-Fog-Edge continuum.
 * Placed in the marketplace package to keep tests self-contained.
 * * Structure:
 * - Cloud_Core (0,0 -> 1000,1000)
 * |
 * +-- Fog_London (0,0 -> 500,500)
 * |
 * +-- Edge_Westminster (0,0 -> 100,100)
 */
public class MarketplaceContinuumTopologyFixture implements TopologyFixture {

    private MarketplaceBroker root;

    // We keep references to specific nodes to help debugging or direct access if
    // needed
    private MarketplaceBroker fog;
    private MarketplaceBroker edge;

    @Override
    public void setup(BrokerFactory factory) {
        // We require a BoundedBrokerFactory to set specific physical regions
        if (!(factory instanceof BoundedBrokerFactory)) {
            throw new IllegalArgumentException("Marketplace tests require a BoundedBrokerFactory");
        }
        BoundedBrokerFactory boundedFactory = (BoundedBrokerFactory) factory;

        // 1. Cloud Layer (Root)
        // Covers the entire simulation world (0-1000)
        this.root = (MarketplaceBroker) boundedFactory.createLeafBroker("Cloud_Core",
                new Location(0, 0, 0), new Location(1000, 1000, 0));

        // 2. Fog Layer (Regional)
        // Covers the "City" region (0-500)
        this.fog = (MarketplaceBroker) boundedFactory.createLeafBroker("Fog_London",
                new Location(0, 0, 0), new Location(500, 500, 0));

        // 3. Edge Layer (Local)
        // Covers the "Neighborhood" region (0-100)
        this.edge = (MarketplaceBroker) boundedFactory.createLeafBroker("Edge_Westminster",
                new Location(0, 0, 0), new Location(100, 100, 0));

        // Link the Topology
        this.root.addChild(this.fog);
        this.fog.addChild(this.edge);
    }

    @Override
    public SimulationBroker getRoot() {
        return root;
    }

    @Override
    public String getName() {
        return "Marketplace Continuum (Cloud-Fog-Edge)";
    }

    // --- Standard Recursive Search Methods (Required by Interface) ---

    @Override
    public <T extends TreeNode> T findNode(String name, Class<T> clazz) {
        return findNodeRecursive(root, name, clazz);
    }

    @Override
    public <T extends TreeNode> T findNodeContains(String partialName, Class<T> clazz) {
        return findNodeContainsRecursive(root, partialName, clazz);
    }

    private <T extends TreeNode> T findNodeRecursive(TreeNode node, String name, Class<T> clazz) {
        if (node == null)
            return null;
        if (node.getName().equals(name) && clazz.isInstance(node)) {
            return clazz.cast(node);
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                T result = findNodeRecursive(child, name, clazz);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private <T extends TreeNode> T findNodeContainsRecursive(TreeNode node, String partialName, Class<T> clazz) {
        if (node == null)
            return null;
        if (node.getName().contains(partialName) && clazz.isInstance(node)) {
            return clazz.cast(node);
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                T result = findNodeContainsRecursive(child, partialName, clazz);
                if (result != null)
                    return result;
            }
        }
        return null;
    }
}
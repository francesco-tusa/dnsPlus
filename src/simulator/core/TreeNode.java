package simulator.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeNode implements LoggableEntity {

    private String name;
    private TreeNode parent;
    private List<TreeNode> children = null;
    private int nodeLevel; 

    public TreeNode(String name) {
        this.name = name;
        this.parent = null;
        this.nodeLevel = 0;
    }

    public TreeNode(TreeNode node) {
        this.name = node.name;
        this.parent = node.parent;
        this.nodeLevel = node.nodeLevel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TreeNode getParent() {
        return parent;
    }

    public List<TreeNode> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }
    
    public int getNodeLevel() {
        return nodeLevel;
    }

    public void addChild(TreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        child.parent = this;
        child.nodeLevel = this.nodeLevel + 1;
        this.children.add(child);
    }

    public void removeChild(TreeNode child) {
        if (this.children != null) {
            child.parent = null;
            this.children.remove(child);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    // --- LoggableEntity Implementation ---

    /**
     * Default implementation: Logs the receiver's location info.
     * Overridden by Publishers to force logging their own location.
     */
    @Override
    public String resolveLogLocation(String receiverLocationInfo) {
        return receiverLocationInfo;
    }

    /**
     * Default implementation: No physical location.
     * Overridden by Publishers/Subscribers to return their Location.
     */
    @Override
    public Location getMetricLocation() {
        return null;
    }
}
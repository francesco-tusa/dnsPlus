package simulator.core;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    private String name;
    private TreeNode parent;
    private List<TreeNode> children;
    private int nodeLevel; // Re-added for deterministic visualisation layout

    public TreeNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.parent = null;
        this.nodeLevel = 0; // Root is at level 0
    }

    public TreeNode(TreeNode node) {
        this.name = node.name;
        this.children = new ArrayList<>();
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
        return children;
    }
    
    public int getNodeLevel() {
        return nodeLevel;
    }

    public void addChild(TreeNode child) {
        child.parent = this;
        child.nodeLevel = this.nodeLevel + 1; // Set level of child
        this.children.add(child);
    }

    public void removeChild(TreeNode child) {
        child.parent = null;
        this.children.remove(child);
    }

    @Override
    public String toString() {
        return name;
    }
}

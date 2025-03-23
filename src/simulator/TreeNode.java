package simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TreeNode {

    private List<TreeNode> children;
    private TreeNode parent;

    public TreeNode(TreeNode value) {
        this.children = new ArrayList<>();
    }

    public TreeNode() {
        this.children = new ArrayList<>();
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public TreeNode getParent() {
        return parent;
    }
    
    public void addChild(TreeNode child) {
        child.parent = this;
        this.children.add(child);
    }

    public void removeChild(TreeNode child) {
        child.parent = null;
        this.children.remove(child);
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode treeNode = (TreeNode) o;
        return Objects.equals(parent, treeNode.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }
}
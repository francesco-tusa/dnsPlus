package simulator;

import java.util.ArrayList;
import java.util.List;

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
}
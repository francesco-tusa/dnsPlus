package simulator;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    private String name;
    private TreeNode parent;
    private List<TreeNode> children;


    public TreeNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public TreeNode getParent() {
        return parent;
    }

    public List<TreeNode> getChildren() {
        return children;
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
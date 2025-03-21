package simulator;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    List<TreeNode> children;

    //TreeNode value;
    TreeNode parent;

    public List<TreeNode> getChildren() {
        return children;
    }

    public TreeNode getParent() {
        return parent;
    }
    
    
    

    public TreeNode(TreeNode value) {
        //this.value = value;

        this.children = new ArrayList<>();
    }

    public TreeNode() {
        //this.value = this;
        this.children = new ArrayList<>();
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

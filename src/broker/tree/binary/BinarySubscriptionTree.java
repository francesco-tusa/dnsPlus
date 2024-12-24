package broker.tree.binary;

import broker.tree.SubscriptionTree;
import subscribing.Subscription;
import publishing.Publication;

/**
 *
 * @author uceeftu
 */

 
public class BinarySubscriptionTree implements SubscriptionTree 
{    
    private SubscriptionBinaryTreeNode root;
    private BrokerWithBinaryTree broker;
    
    public BinarySubscriptionTree(BrokerWithBinaryTree b)
    {
        broker = b;
    }

    public SubscriptionBinaryTreeNode getRoot() {
        return root;
    }
 
    
    @Override
    public Subscription addNode(Subscription s) 
    {
        SubscriptionBinaryTreeNode newNode = new SubscriptionBinaryTreeNode(s);
 
        if (root == null) 
        {
            root = newNode;
            return null;
        } 
    
        else 
        {
            SubscriptionBinaryTreeNode focusNode = root;
            SubscriptionBinaryTreeNode parent;
 
            while (true) 
            {
                parent = focusNode;
                
                if (broker.match(s, focusNode.subscription) == 0) 
                {
                    //debug
                    //System.out.println("equals");
                    //System.out.println("s: " + s.getServiceName());
                    //System.out.println("focus: " + focusNode.subscription.getServiceName());
                    //System.out.println("");
                    
                    return focusNode.subscription;
                }
                
                else if (broker.match(s, focusNode.subscription) < 0)
                {
                    //debug
                    //System.out.println("s < focusNode");
                    //System.out.println("s: " + s.getServiceName());
                    //System.out.println("focus: " + focusNode.subscription.getServiceName());
                    //System.out.println("");
                    
                    focusNode = focusNode.left;
                    if (focusNode == null) 
                    {
                        parent.left = newNode;
                        return null;
                    }
                } 

                else 
                {
                    //System.out.println("s > focusNode");
                    //System.out.println("s: " + s.getServiceName());
                    //System.out.println("focus: " + focusNode.subscription.getServiceName());
                    //System.out.println("");
                    
                    focusNode = focusNode.right;
                    if (focusNode == null) 
                    {
                        parent.right = newNode;
                        return null;
                    }
                }
            }
        }
    }
    
    
    @Override
    public Subscription search(Publication p) {
        return searchRecursive(root, p);
    }

    private Subscription searchRecursive(SubscriptionBinaryTreeNode node, Publication p) {
        // Base Cases: node is null or key is present at node
        if (node == null) {
            return null;
        }

        if (broker.match(p, node.subscription) == 0) {
            return node.getSubscription();
        }

        // Key is greater than node's key
        if (broker.match(p, node.subscription) > 0) {
            return searchRecursive(node.right, p);
        }

        // Key is smaller than node's key
        return searchRecursive(node.left, p);
    }
}
package broker.binarytree;

import broker.Broker;
import subscribing.Subscription;
import publishing.Publication;

/**
 *
 * @author uceeftu
 */

 
public class SubscriptionBinaryTree 
{    
    private SubscriptionBinaryTreeNode root;
    private Broker broker;
    
    public SubscriptionBinaryTree(Broker b)
    {
        broker = b;
    }

    public SubscriptionBinaryTreeNode getRoot() {
        return root;
    }
 
    
    

    public void addNode(Subscription s) 
    {
        SubscriptionBinaryTreeNode newNode = new SubscriptionBinaryTreeNode(s);
 
        if (root == null) 
        {
            root = newNode;
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
                    
                    return;
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
                        return;
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
                        return;
                    }
                }
            }
        }
    }
    
    
    public SubscriptionBinaryTreeNode search(SubscriptionBinaryTreeNode root, Publication p)
    {
	// Base Cases: root is null or key is present at root
	if (root == null || broker.match(p, root.subscription) == 0) {
            return root;
        }
            
	// Key is greater than root's key
        if (broker.match(p, root.subscription) > 0) 
        {
            //System.out.println("p > root: " + root.subscription.getServiceName());
            return search(root.right, p);
        }

        // Key is smaller than root's key
        //System.out.println("p < root: " + root.subscription.getServiceName());
	return search(root.left, p);
    }
}
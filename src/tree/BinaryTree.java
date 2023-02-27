package tree;

import naming.Broker;
import naming.Subscription;
import naming.Publication;

/**
 *
 * @author uceeftu
 */

 
public class BinaryTree 
{    
    private Node root;
    private Broker broker;
    
    public BinaryTree(Broker b)
    {
        broker = b;
    }

    public Node getRoot() {
        return root;
    }
 
    
    
    
    public void addNode(Subscription s) 
    {
        Node newNode = new Node(s);
 
        if (root == null) 
        {
            root = newNode;
        } 
    
        else 
        {
            Node focusNode = root;
            Node parent;
 
            while (true) 
            {
                parent = focusNode;
                
                if (broker.cover(s, focusNode.subscription) == 0) 
                {
                    //debug
                    System.out.println("equals");
                    System.out.println("s: " + s.getServiceName());
                    System.out.println("focus: " + focusNode.subscription.getServiceName());
                    return;
                }
                
                else if (broker.cover(s, focusNode.subscription) < 0)
                {
                    //debug
                    System.out.println("s < focusNode");
                    System.out.println("s: " + s.getServiceName());
                    System.out.println("focus: " + focusNode.subscription.getServiceName());
                    
                    focusNode = focusNode.left;
                    if (focusNode == null) 
                    {
                        parent.left = newNode;
                        return;
                    }
                } 

                else 
                {
                    System.out.println("s > focusNode");
                    System.out.println("s: " + s.getServiceName());
                    System.out.println("focus: " + focusNode.subscription.getServiceName());
                    
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
    
 
  
    public Node search(Node root, Publication p)
    {
	// Base Cases: root is null or key is present at root
	if (root == null || broker.match(p, root.subscription) == 0) {
            return root;
        }
            
	// Key is greater than root's key
        if (broker.match(p, root.subscription) > 0) 
        {
            System.out.println(" p > root: " + root.subscription.getServiceName());
            return search(root.right, p);
        }

        System.out.println(" p < root: " + root.subscription.getServiceName());
	// Key is smaller than root's key
	return search(root.left, p);
    }
   
    
}
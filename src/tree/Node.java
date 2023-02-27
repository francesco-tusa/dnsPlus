package tree;

import naming.Subscription;

/**
 *
 * @author uceeftu
 */
public class Node 
{
    Subscription subscription;
    Node left;
    Node right;
 
    public Node(Subscription s) {
        subscription = s;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
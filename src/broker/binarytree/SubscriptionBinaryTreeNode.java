package broker.binarytree;

import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SubscriptionBinaryTreeNode 
{
    Subscription subscription;
    SubscriptionBinaryTreeNode left;
    SubscriptionBinaryTreeNode right;
 
    public SubscriptionBinaryTreeNode(Subscription s) {
        subscription = s;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
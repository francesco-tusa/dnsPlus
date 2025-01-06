package subscribing;

/**
 *
 * @author uceeftu
 */
public class PoisonPillSubscription extends Subscription {
    
    public PoisonPillSubscription(String subscriber) {
        super(subscriber);
    }
    
    public String getSubscriber() {
        return getSubscribers().getFirst();
    }

}

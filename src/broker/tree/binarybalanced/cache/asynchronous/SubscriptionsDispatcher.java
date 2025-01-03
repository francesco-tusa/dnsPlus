package broker.tree.binarybalanced.cache.asynchronous;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.Publication;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class SubscriptionsDispatcher {

    private static final Logger logger = CustomLogger.getLogger(SubscriptionsDispatcher.class.getName());
    private Map<String, AsynchronousSubscriber> subscribers = Collections.synchronizedMap(new HashMap<>());
    private Thread dispatchingThread;
    private SubscriptionProcessor subscriptionProcessor;

    public SubscriptionsDispatcher(SubscriptionProcessor subscriptionProcessor) {
        this.subscriptionProcessor = subscriptionProcessor;
    }

    public void startDispatching() {
        dispatchingThread = new Thread(new Dispatching(), "Dispatcher");
        dispatchingThread.start();
    }

    public void addSubscriber(AsynchronousSubscriber subscriber) {
        subscribers.putIfAbsent(subscriber.getName(), subscriber);
    }

    private class Dispatching implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Publication p = subscriptionProcessor.getMatchResult();
                    logger.log(Level.WARNING, "Publication recipients: {0}",  p.getRecipients());
                    dispatch(p);
                } catch (InterruptedException ex) {
                    logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
                }
            }
        }
        
        private void dispatch(Publication p) {
            for (String subscriberName : p.getRecipients()) {
                AsynchronousSubscriber subscriber = subscribers.get(subscriberName);
                subscriber.addMatchingPublication(p);
            }
        }
    }
    
}

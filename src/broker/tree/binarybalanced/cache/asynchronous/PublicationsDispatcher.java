package broker.tree.binarybalanced.cache.asynchronous;

import broker.AsynchronousBroker;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.PoisonPillPublication;
import publishing.Publication;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class PublicationsDispatcher {

    private static final Logger logger = CustomLogger.getLogger(PublicationsDispatcher.class.getName());
    private Map<String, AsynchronousSubscriber> subscribers = Collections.synchronizedMap(new HashMap<>());
    private Thread dispatchingThread;
    AsynchronousBroker broker;

    public PublicationsDispatcher(AsynchronousBroker b) {
        broker = b;
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
            logger.log(Level.FINE, "Started Dispatching Thread");
            while (true) {

                Publication p = broker.getSubscriptionResult();

                if (p != null) {
                    if (p instanceof PoisonPillPublication poisonPill) {
                        //logger.log(Level.SEVERE, "Publication Queue size: {0}", ((AsynchronousBrokerWithBinaryBalancedTreeAndCache) broker).getSubscriptionProcessor().getPublicationsQueueSize());
                        logger.log(Level.WARNING, "Broadbasting a poison pill publication to all registered subscribers");
                        broadcast(poisonPill);
                        break;
                    }

                    logger.log(Level.FINER, "Publication for {0} is being dispatched", p.getServiceName());
                    logger.log(Level.FINEST, "Publication recipients: {0}", p.getRecipients());
                    dispatch(p);
                }
            }
        }
        
        
        private void broadcast(PoisonPillPublication p) {
            for (String subscriberName : subscribers.keySet()) {
                AsynchronousSubscriber subscriber = subscribers.get(subscriberName);
                subscriber.addMatchingPublication(p);
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

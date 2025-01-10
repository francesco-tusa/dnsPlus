package publishing;

import subscribing.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.AsynchronousBroker;
import subscribing.PoisonPillSubscription;

// Used for debug: receives the subscriptions that matched its publications
public final class ReceivingPublisher extends Publisher {
    
    private static final Logger logger = CustomLogger.getLogger(ReceivingPublisher.class.getName());
    

    public ReceivingPublisher(String name, AsynchronousBroker broker) {
        super(name, broker);
        init();
    }
    
    @Override
    public final void init() {
        super.init();
    }
    
    
    // for debugging
    public void receiveSubscriptions() {
        Thread publisherReceiverThread = new Thread(() -> listenForSubscriptions(), getName() + "receiver");
        publisherReceiverThread.start();
    }
    
    private void listenForSubscriptions() {
        logger.log(Level.INFO, "*** Waiting for results (subscriptions that matched sent publications) ***");
        while (true) {
            Subscription s = ((AsynchronousBroker) getBroker()).getPublicationResult();
            if (s instanceof PoisonPillSubscription) {
                logger.log(Level.WARNING, "publisherReceiver thread being stopped");
                break;
            }
            logger.log(Level.INFO, "The Broker Found a Subscription matching {0}", s.getServiceName());
        }
    }
}
package publishing;

import subscribing.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.AsynchronousBroker;
import subscribing.PoisonPillSubscription;


public final class AsynchronousPublisher extends Publisher {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousPublisher.class.getName());
    

    public AsynchronousPublisher(String name, AsynchronousBroker broker) {
        super(name, broker);
    }
    
    @Override
    public void init() {
        super.init();
        receiveSubscriptions();
    }
    
    
    // for debugging
    private void receiveSubscriptions() {
        Thread publisherReceiverThread = new Thread(() -> listenForSubscriptions());
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
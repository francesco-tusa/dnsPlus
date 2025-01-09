package subscribing;

import broker.AsynchronousBroker;
import publishing.Publication;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.PoisonPillPublication;


public class AsynchronousSubscriber extends Subscriber {
    private static final Logger logger = CustomLogger.getLogger(Subscriber.class.getName());
    
    private BlockingQueue<Publication> publicationsQueue;
    
    public AsynchronousSubscriber(String name, AsynchronousBroker broker) {
        super(name, broker);
        publicationsQueue = new LinkedBlockingQueue<>();
        init();
    }
    
    public AsynchronousSubscriber(String name) {
        this(name, null);
    }
    
    @Override
    public final void init() {
        super.init();
        
        if (getBroker() != null) {
            ((AsynchronousBroker) getBroker()).registerSubscriber(this);
        }
        
        receivePublications();
    }
    
    public void setBroker(AsynchronousBroker broker) {
        // we set the broker only if not previously done
        // when calling this object constructor
        if (getBroker() == null) {
            super.setBroker(broker);
            broker.registerSubscriber(this);
        }
    }
    
    public void addMatchingPublication(Publication p) {
        logger.log(Level.FINE, "Added publication for {0} to Subscriber {1}'s Queue", new Object[]{p.getServiceName(), getName()});
        publicationsQueue.offer(p);
    }
    
    private void receivePublications() {
        Thread subscriberReceiverThread = new Thread(() -> listenForPublications(), getName() + "receiver");
        subscriberReceiverThread.start();
    }
    
    
    private void listenForPublications() {
        logger.log(Level.INFO, "*** Subscriber {0} is waiting for results (publications that matched their subscriptions) ***", getName());
        while (true) {
            try {
                //Publication p = broker.getSubscriptionResult();
                Publication p = publicationsQueue.take();
                if (p instanceof PoisonPillPublication) {
                    logger.log(Level.WARNING, "subscriberReceiver thread for {0} being stopped", getName());
                    break;
                }
                logger.log(Level.INFO, "The Broker Found and Returned a Matching Publication {0} for {1}", new Object[]{p.getServiceName(), getName()});
            } catch (InterruptedException ex) {
                 logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
            }
        } 
    }
}
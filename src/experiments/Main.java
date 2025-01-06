package experiments;

import experiments.cache.DNSWithCacheExperiment;
import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;
/**
 *
 * @author uceeftu
 */
public class Main {
    
    private static final Logger logger = CustomLogger.getLogger(Experiment.class.getName(), Level.INFO);
    
    
    public static void main(String[] args) {
        //Experiment experiment = new DNSWithCacheAsynchronousExperiment("websites.txt", 1);
        Experiment experiment1 = new DNSWithCacheAsynchronousExperiment("websites.txt", 1, 1000, 100, 2, 3);
        //Experiment experiment = new DNSWithCacheExperiment("websites.txt", 1);
        //Experiment experiment2 = new DNSWithCacheExperiment("websites.txt", 1, 1000, 100);
        
        experiment1.start();  
        //experiment2.start();   
    }
    
}

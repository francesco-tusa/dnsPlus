package experiments;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class ExperimentParallelTask implements Runnable {
    
    protected String name;
    protected Duration duration;
    //private static final Logger logger = CustomLogger.getLogger(ConcurrentDNSWithCacheRunPublisherTask.class.getName());

    public ExperimentParallelTask() {
    }

    public String getName() {
        return name;
    }

    public long getDuration() {
        return duration.toMillis();
    }

    @Override
    public abstract void run();
    
}

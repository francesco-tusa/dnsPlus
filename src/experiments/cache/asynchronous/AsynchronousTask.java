package experiments.cache.asynchronous;

import experiments.SynchronousTask;
import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousTask extends SynchronousTask {
    private Duration asynchronousProcessingDuration;

    protected AsynchronousTask() {
        asynchronousProcessingDuration = Duration.ZERO;
    }
    
    @Override
    public long getReplyDuration() {
        return asynchronousProcessingDuration.toMillis();
    }

    @Override
    public void setAsynchronousProcessingDuration(Duration asynchronousProcessingDuration) {
        this.asynchronousProcessingDuration = asynchronousProcessingDuration;
    } 
    
    @Override
    public abstract void run();
    
    public abstract void cleanUp();
}

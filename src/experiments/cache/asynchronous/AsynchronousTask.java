package experiments.cache.asynchronous;

import experiments.SynchronousTask;
import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousTask extends SynchronousTask {
    private Duration replyDuration;

    protected AsynchronousTask() {
        replyDuration = Duration.ZERO;
    }
    
    @Override
    public long getReplyDuration() {
        return replyDuration.toMillis();
    }

    @Override
    public void setReplyDuration(Duration replyDuration) {
        this.replyDuration = replyDuration;
    } 
    
    @Override
    public abstract void run();
    
    public abstract void cleanUp();
}

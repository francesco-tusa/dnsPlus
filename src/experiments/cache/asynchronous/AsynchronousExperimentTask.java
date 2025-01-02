package experiments.cache.asynchronous;

import experiments.ExperimentTask;
import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousExperimentTask extends ExperimentTask {
    private Duration replyDuration;

    public long getReplyDuration() {
        return replyDuration.toMillis();
    }

    protected void setReplyDuration(Duration replyDuration) {
        this.replyDuration = replyDuration;
    } 
    
    @Override
    public abstract void run();
}

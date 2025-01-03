package experiments.cache.asynchronous;

import experiments.RunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousTasksRunner extends RunTasksExecutor {
    void waitForRequestsCompletion();
    void waitForRepliesCompletion();
}

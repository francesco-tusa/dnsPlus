package experiments.cache.asynchronous;

import experiments.RunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousRunTasksExecutor extends RunTasksExecutor {
    void waitForRequestsCompletion();
    void waitForRepliesCompletion();
}
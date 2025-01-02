package experiments.cache.asynchronous;

import experiments.Run;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousRun extends Run<AsynchronousTask> {

    void waitForRequestsCompletion();

    void waitForRepliesCompletion();
}

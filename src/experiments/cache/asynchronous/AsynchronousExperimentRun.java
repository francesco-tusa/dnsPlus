package experiments.cache.asynchronous;

import experiments.ExperimentRun;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousExperimentRun extends ExperimentRun<AsynchronousExperimentTask> {

    void waitForRequestsCompletion();

    void waitForRepliesCompletion();
}

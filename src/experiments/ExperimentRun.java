package experiments;

import java.util.List;

/**
 *
 * @author uceeftu
 */
interface ExperimentRun {
    String getName();
    void setUp();
    void start();
    void cleanUp();
    void waitForCompletion();
    void addTask(ExperimentParallelTask t);
    List<ExperimentParallelTask> getTasks();
}

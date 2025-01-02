package experiments;

import java.util.List;

/**
 *
 * @author uceeftu
 * @param <T>
 */
public interface ExperimentRun<T extends ExperimentTask> {
    
    String getName();

    void setUp();

    void start();

    void cleanUp();

    void addTask(T task);

    List<T> getTasks();
    
}

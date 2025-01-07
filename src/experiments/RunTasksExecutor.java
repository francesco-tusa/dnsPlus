package experiments;

/**
 *
 * @author uceeftu
 */
public interface RunTasksExecutor extends RunTasksOutputManager {
    void setUp();
    void addTask(Task task);
    void start();
    void cleanUp();
    void finalise();
}

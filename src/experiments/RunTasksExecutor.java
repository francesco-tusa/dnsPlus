package experiments;

/**
 *
 * @author uceeftu
 */
public interface RunTasksExecutor extends RunTasksOutputManager{
    void setUp();
    void start();
    void cleanUp();
    void addTask(Task task);
}

package experiments;

/**
 *
 * @author uceeftu
 */
public interface RunTasksExecutor extends RunTasksOutputManager {
    void setUp();
    void addTask(Task task);
    void runTasks();
    void cleanUp();
    void finalise();
    void executeRun();
}

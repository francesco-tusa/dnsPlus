package experiments;

import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class RunSequentialTasksExecutor extends DefaultRunTasksExecutor {
    
     private static final Logger logger = CustomLogger.getLogger(RunSequentialTasksExecutor.class.getName());

    public RunSequentialTasksExecutor(String name) {
        super(name);
    }

    @Override
    public void start() {
        for (Task task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
            try {
                t.join();
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
            }
        }
    }

}

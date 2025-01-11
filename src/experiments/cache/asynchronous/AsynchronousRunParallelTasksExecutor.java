package experiments.cache.asynchronous;

import experiments.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousRunParallelTasksExecutor extends RunParallelTasksExecutor implements AsynchronousRunTasksExecutor {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousRunParallelTasksExecutor.class.getName());
    
    private CountDownLatch requestsLatch;
    private CountDownLatch repliesLatch;
    
    public AsynchronousRunParallelTasksExecutor(String name) {
        super(name);
    }

    protected CountDownLatch getRequestsLatch() {
        return requestsLatch;
    }

    protected CountDownLatch getRepliesLatch() {
        return repliesLatch;
    }

    protected void setRequestsLatch(CountDownLatch requestsLatch) {
        this.requestsLatch = requestsLatch;
    }

    protected void setRepliesLatch(CountDownLatch repliesLatch) {
        this.repliesLatch = repliesLatch;
    }
    
    
    
    @Override
    public void runTasks() {
        logger.log(Level.SEVERE, "Running Tasks");
        List<Task> tasks = getTasks();
        requestsLatch = new CountDownLatch(tasks.size());
        repliesLatch = new CountDownLatch(tasks.size());
        for (Task task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
        }       
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Cleaning Up Run");
        List<Task> tasks = getTasks();
        for (Task task : tasks) {
            if (task instanceof AsynchronousTask asyncTask) {
                asyncTask.cleanUp();
            }
        }
    }
    
    @Override
    public void executeRun() {
        runTasks();
        waitForRequestsCompletion();
        cleanUp();
        waitForRepliesCompletion();
    }
    
    
    @Override
    public void waitForRequestsCompletion() {
        try {
            requestsLatch.await();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void waitForRepliesCompletion() {
        try {
            repliesLatch.await();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void setTaskRequestCompleted() {
        getRequestsLatch().countDown();
    }

    @Override
    public void setTaskResponseReceived() {
        getRepliesLatch().countDown();
    }
}

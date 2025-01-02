package experiments.outputdata.asynchronous;

import experiments.cache.asynchronous.AsynchronousTask;
import experiments.outputdata.RunOutput;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public class AsynchronousRunOutput extends RunOutput<AsynchronousTask> {
    
    public AsynchronousRunOutput(String name, List<AsynchronousTask> tasks) {
        super(name, tasks);
    }
}

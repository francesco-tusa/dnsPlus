package experiments.cache.asynchronous;

import experiments.Experiment;
import experiments.outputdata.asynchronous.AsynchronousExperimentOutput;
import experiments.outputdata.asynchronous.AsynchronousRunOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousExperiment extends Experiment {
    
    private List<AsynchronousRunOutput> experimentRunsOutput;
    private AsynchronousExperimentOutput experimentOutput;

    public AsynchronousExperiment(String name, String inputFileName, int numberOfRuns) {
        super(name, inputFileName, numberOfRuns);
        experimentRunsOutput = new ArrayList<>(numberOfRuns);
    }

    public List<AsynchronousRunOutput> getExperimentRunsOutput() {
        return experimentRunsOutput;
    }

    @Override
    protected abstract void executeRun();

    @Override
    public void calculateStats() {
        experimentOutput = new AsynchronousExperimentOutput(getName(), experimentRunsOutput);
        experimentOutput.calculateStats();
    }

    @Override
    public String toString() {
        return "{name=" + getName() + ", inputFileName=" + getInputFileName() + ", numberOfRuns=" + getNumberOfRuns() + ", \n\texperimentOutput=" + experimentOutput + '}';
    }
    
}

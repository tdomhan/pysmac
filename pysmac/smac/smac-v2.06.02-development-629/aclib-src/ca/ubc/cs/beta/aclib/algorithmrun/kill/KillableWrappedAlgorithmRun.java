package ca.ubc.cs.beta.aclib.algorithmrun.kill;

import ca.ubc.cs.beta.aclib.algorithmrun.AbstractAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * Wrapper class that wraps existing {@link ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun} instances, providing a noop kill().
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class KillableWrappedAlgorithmRun implements KillableAlgorithmRun {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8430387600111279170L;
	
	private final AlgorithmRun run;
	
	public KillableWrappedAlgorithmRun(AlgorithmRun run)
	{
		if(run.getRunResult().equals(RunResult.RUNNING))
		{
			throw new IllegalStateException("Cannot wrap a run that isn't completed");
		}
		this.run = run;
	}
	
	public AlgorithmExecutionConfig getExecutionConfig() {
		return run.getExecutionConfig();
	}

	public RunConfig getRunConfig() {
		return run.getRunConfig();
	}

	public RunResult getRunResult() {
		return run.getRunResult();
	}

	public double getRuntime() {
		return run.getRuntime();
	}

	public double getRunLength() {
		return run.getRunLength();
	}

	public double getQuality() {
		return run.getQuality();
	}

	public long getResultSeed() {
		return run.getResultSeed();
	}

	public String getResultLine() {
		return run.getResultLine();
	}

	public String getAdditionalRunData() {
		return run.getAdditionalRunData();
	}

	public void run() {
		run.run();
	}

	public Object call() {
		return run.call();
	}

	public boolean isRunResultWellFormed() {
		return run.isRunResultWellFormed();
	}

	public String rawResultLine() {
		return run.rawResultLine();
	}

	public double getWallclockExecutionTime() {
		return run.getWallclockExecutionTime();
	}

	@Override
	public void kill() {
		//NOOP this run is done
	}

	@Override
	public boolean isRunCompleted() {

		return run.isRunCompleted();
	}

	@Override
	public final int hashCode()
	{
		return run.hashCode();
	}
	
	/**
	 * Two AlgorithmRuns are considered equal if they have same runConfig and execConfig
	 */
	@Override
	public final boolean equals(Object o)
	{
		return run.equals(o);
	}
	
	@Override
	public final String toString()	
	{
		return AbstractAlgorithmRun.toString(this);
	}
}

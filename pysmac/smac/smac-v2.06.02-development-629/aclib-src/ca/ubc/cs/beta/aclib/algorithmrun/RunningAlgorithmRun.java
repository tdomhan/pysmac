package ca.ubc.cs.beta.aclib.algorithmrun;

import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillHandler;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * AlgorithmRun that reports that it's current status is RUNNING.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class RunningAlgorithmRun extends ExistingAlgorithmRun implements KillableAlgorithmRun {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5427091882882378946L;
	private final KillHandler handler;


	public RunningAlgorithmRun(AlgorithmExecutionConfig execConfig,
			RunConfig runConfig, double runtime, double runlength, double quality, long seed, double walltime, KillHandler handler) {
		super(execConfig, runConfig, RunResult.RUNNING, runtime, runlength, quality, seed, walltime);
		this.handler = handler;
	}

	@Deprecated
	/**
	 * @deprecated  the constructor that doesn't take a result string is preferred.
	 */
	public RunningAlgorithmRun(AlgorithmExecutionConfig execConfig,
			RunConfig runConfig, String result, KillHandler handler) {
		super(execConfig, runConfig, result);
		this.handler = handler;
	}

	@Override
	public void kill() {
		handler.kill();
		
	}

}

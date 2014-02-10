package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
/**
 * Abstract Decorator for {@link ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator} that provides a template method that subtypes can use to replace or notified about each run 
 * 
 * 
 *  
 * @author Steve Ramage 
 *
 */
public abstract class AbstractForEachRunTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	

	public AbstractForEachRunTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae)
	{
		super(tae);
	}
	

	@Override
	public final List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return processRuns(tae.evaluateRun(processRunConfigs(runConfigs), obs));
	}

	@Override
	public final void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		//We need to make sure wrapped versions are called in the same order
		//as there unwrapped versions.
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
					runs = processRuns(runs);			
					handler.onSuccess(runs);
			}

			@Override
			public void onFailure(RuntimeException t) {
					handler.onFailure(t);
			}
		};
		
		tae.evaluateRunsAsync(processRunConfigs(runConfigs), myHandler, obs);

	}

	
	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected AlgorithmRun processRun(AlgorithmRun run)
	{
		return run;
	}
	
	/**
	 * Template method that is invoked with each runConfig that we request
	 * @param rc the runconfig  being requested
	 * @return runConfig object to replace the run
	 */
	protected RunConfig processRun(RunConfig rc)
	{
		return rc;
	}
	
	
	protected final List<AlgorithmRun> processRuns(List<AlgorithmRun> runs)
	{
		for(int i=0; i < runs.size(); i++)
		{
			runs.set(i, processRun(runs.get(i)));
		}
		
		return runs;
	}
	
	protected final List<RunConfig> processRunConfigs(List<RunConfig> runConfigs)
	{	
		runConfigs = new ArrayList<RunConfig>(runConfigs);
		for(int i=0; i < runConfigs.size(); i++)
		{
			runConfigs.set(i, processRun(runConfigs.get(i)));
		}
		return runConfigs;
	}
	
}

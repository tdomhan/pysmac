package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety;

import java.util.List;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Modifies Target Algorithm Evaluators to treat CRASHES as aborts
 * @author Steve Ramage 
 *
 */
@ThreadSafe
public class AbortOnCrashTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public AbortOnCrashTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);
		
	}
	
	

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return validate(super.evaluateRun(runConfigs, obs));
	}
	

	private List<AlgorithmRun> validate(List<AlgorithmRun> runs)
	{
		
		for(AlgorithmRun run : runs)
		{
			if(run.getRunResult().equals(RunResult.CRASHED))
			{
				throw new TargetAlgorithmAbortException("Target Algorithm Run Reported Crashed: " + run.toString());
			}
		}
		return runs;
	}
	
	
	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
				try {
					validate(runs);
					handler.onSuccess(runs);
				} catch(TargetAlgorithmAbortException e)
				{
					handler.onFailure(e);
				}
				
			}

			@Override
			public void onFailure(RuntimeException t) {
				handler.onFailure(t);
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, myHandler, obs);
	}

}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

/**
 * Handler interface for Deferred Target Algorithm Evaluator runs
 * <p>
 * <b>Client Note:</b> If the onSuccess() method throws an exception, you should call the onFailure() method,
 * this primarily simplifies the implementations of decorators.
 * 
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface TargetAlgorithmEvaluatorCallback {

	/**
	 * Invoked if/when the runs complete
	 * @param runs the list of completed runs
	 */
	public void onSuccess(List<AlgorithmRun> runs);
	
	/**
	 * Invoked if/when there is a failure
	 * @param e throwable that occurred
	 */
	public void onFailure(RuntimeException e);
	
	
	
}

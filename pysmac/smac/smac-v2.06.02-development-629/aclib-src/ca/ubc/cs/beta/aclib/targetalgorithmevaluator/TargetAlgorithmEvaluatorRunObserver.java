package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;

public interface TargetAlgorithmEvaluatorRunObserver {

	/**
	 * Invoked on a best effort basis when new information is available
	 * @param runs
	 */
	public void currentStatus(List<? extends KillableAlgorithmRun> runs);
}

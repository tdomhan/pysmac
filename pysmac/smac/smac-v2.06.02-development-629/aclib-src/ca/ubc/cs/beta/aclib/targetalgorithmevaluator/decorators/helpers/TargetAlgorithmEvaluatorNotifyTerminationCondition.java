package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class TargetAlgorithmEvaluatorNotifyTerminationCondition extends
		AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	private final EventManager evtManager;
	private boolean flush;
	private TerminationCondition termCond;
	

	public TargetAlgorithmEvaluatorNotifyTerminationCondition(TargetAlgorithmEvaluator tae, EventManager evtManager, TerminationCondition termCond, boolean flush) {
		super(tae);
		this.evtManager = evtManager;
		this.flush = flush;
		this.termCond = termCond;
		
	}

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected synchronized AlgorithmRun processRun(AlgorithmRun run)
	{
		termCond.notifyRun(run);
		evtManager.fireEvent(new AlgorithmRunCompletedEvent(termCond, run));
		if(flush)
		{
			evtManager.flush();
		}
		return run;
	}
	
	
}

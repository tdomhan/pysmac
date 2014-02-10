package ca.ubc.cs.beta.aclib.termination.standard;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public abstract class AbstractTerminationCondition implements TerminationCondition {


	@Override
	public void registerWithEventManager(EventManager evtManager) {
		//noop
	}

	@Override
	public void notifyRun(AlgorithmRun run) {
		//noop
	}

	@Override
	public double getTunerTime()
	{
		return Double.MIN_VALUE;
	}
	
	@Override
	public double getWallTime()
	{
		return Double.MIN_VALUE;
	}
}

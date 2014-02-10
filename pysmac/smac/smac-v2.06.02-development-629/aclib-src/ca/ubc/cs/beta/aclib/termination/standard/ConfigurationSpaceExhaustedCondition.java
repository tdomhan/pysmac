package ca.ubc.cs.beta.aclib.termination.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aclib.termination.ConditionType;
import ca.ubc.cs.beta.aclib.termination.ValueMaxStatus;

public class ConfigurationSpaceExhaustedCondition extends AbstractTerminationCondition implements EventHandler<AlgorithmRunCompletedEvent> {

	private final double runLimit;
	private final AtomicLong algorithmRuns = new AtomicLong(0);
	private final String NAME = "CONFIG_SPACE";
	private final int runsPerConfiguration;
	private final double configSpaceSize;
	
	public ConfigurationSpaceExhaustedCondition(ParamConfigurationSpace configSpace, int runsPerConfiguration)
	{
		this.runLimit = configSpace.getUpperBoundOnSize() * runsPerConfiguration;
		this.runsPerConfiguration = runsPerConfiguration;
		this.configSpaceSize = configSpace.getUpperBoundOnSize();
		
	}
	
	@Override
	public boolean haveToStop() {
		if(this.runLimit  <= algorithmRuns.get())
		{
			return true;
		} else
		{
			return false;
		}
	}


	@Override
	public void handleEvent(AlgorithmRunCompletedEvent event) {
		algorithmRuns.incrementAndGet();
	}
	
	@Override
	public void notifyRun(AlgorithmRun run)
	{
		algorithmRuns.incrementAndGet();
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public void registerWithEventManager(EventManager evtManager) {
		//evtManager.registerHandler(AlgorithmRunCompletedEvent.class, this);
	}


	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		long currentStatus = algorithmRuns.get();
		return Collections.singleton(new ValueMaxStatus(ConditionType.NUMBER_OF_RUNS, currentStatus, runLimit, NAME, "Configuration Space Searched " + (  (currentStatus * 100 / (double) this.runLimit))+ " % \n" ));
	}

	
	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "Every possible configuration (" + this.configSpaceSize +") and problem instance seed pair (" + this.runsPerConfiguration + ") has been run ";
		} else
		{
			return "";
		}
	}
	
	
	
	
}

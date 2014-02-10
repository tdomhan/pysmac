package ca.ubc.cs.beta.aclib.termination.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.ChallengeStartEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildEndEvent;
import ca.ubc.cs.beta.aclib.termination.ValueMaxStatus;

@ThreadSafe
public class NoRunsForManyChallengesEvent extends AbstractTerminationCondition implements EventHandler<AutomaticConfiguratorEvent> {

	//private final String NAME = "NUMBER OF RUNS";
	private final  long iterationWithOutRuns;
	private final AtomicLong attemptedChallengesSinceLastRun = new AtomicLong(0);

	public NoRunsForManyChallengesEvent(long iterationsWithoutRun)
	{
		this.iterationWithOutRuns = iterationsWithoutRun;
	}
		

	@Override
	public boolean haveToStop() {
		return (attemptedChallengesSinceLastRun.get() >= iterationWithOutRuns);
			
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.emptySet();
	}

	@Override
	public void handleEvent(AutomaticConfiguratorEvent event) {
		
		if(event instanceof ChallengeStartEvent)
		{
			attemptedChallengesSinceLastRun.incrementAndGet();
		} else if(event instanceof AlgorithmRunCompletedEvent)
		{
			 attemptedChallengesSinceLastRun.set(0);
		}
		
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public void registerWithEventManager(EventManager evtManager) {
		evtManager.registerHandler(ChallengeStartEvent.class, this);
		evtManager.registerHandler(AlgorithmRunCompletedEvent.class, this);
	}


	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "Too many challenges have been attempted without a successful run (" +  attemptedChallengesSinceLastRun +  ") has been reached";
		} else
		{
			return "";
		}
		
	}
	
}

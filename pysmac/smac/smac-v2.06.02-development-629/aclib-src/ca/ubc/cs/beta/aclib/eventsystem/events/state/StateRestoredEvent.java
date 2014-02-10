package ca.ubc.cs.beta.aclib.eventsystem.events.state;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

/**
 * Event that is fired when state has been restored
 * <br>
 * <b>NOTE:</b> This event should always be flushed when fired, so that everything handling it can get a consistent view of the runHistory.
 *  
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class StateRestoredEvent extends AbstractTimeEvent {

	private int modelsBuilt;
	private ThreadSafeRunHistory runHistory;
	private ParamConfiguration incumbent;

	public StateRestoredEvent(TerminationCondition cond,  int modelsBuilt, ThreadSafeRunHistory runHistory, ParamConfiguration incumbent) {
		super(cond);
		this.modelsBuilt = modelsBuilt;
		this.runHistory = runHistory;
		this.incumbent = incumbent;
		
	}

	public int getModelsBuilt() {
		return modelsBuilt;
	}

	public ThreadSafeRunHistory getRunHistory() {
		return runHistory;
	}
	
	public ParamConfiguration getIncumbent()
	{
		return incumbent;
		
	}

	
	 
	

}

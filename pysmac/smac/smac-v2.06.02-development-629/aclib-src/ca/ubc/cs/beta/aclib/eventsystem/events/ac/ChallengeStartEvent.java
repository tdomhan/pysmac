package ca.ubc.cs.beta.aclib.eventsystem.events.ac;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class ChallengeStartEvent extends AbstractTimeEvent{

	
	private final ParamConfiguration challenger;
	

	public ChallengeStartEvent(TerminationCondition cond, ParamConfiguration challenger) 
	{
		super(cond);
		
		this.challenger = challenger;
		
	}


	public ParamConfiguration getChallenger()
	{
		return challenger;
	}
	
		
}

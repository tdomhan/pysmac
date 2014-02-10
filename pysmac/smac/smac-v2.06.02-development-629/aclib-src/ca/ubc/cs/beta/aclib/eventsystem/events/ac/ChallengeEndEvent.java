package ca.ubc.cs.beta.aclib.eventsystem.events.ac;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class ChallengeEndEvent extends AbstractTimeEvent {

	
	private final ParamConfiguration challenger;
	private final boolean newIncumbent;
	private final int numberOfRuns; 
	
	public ChallengeEndEvent( TerminationCondition cond, ParamConfiguration challenger, boolean newIncumbent, int numberOfRuns) {

		super(cond);
		this.challenger = challenger;
		this.newIncumbent = newIncumbent;
		this.numberOfRuns = numberOfRuns;
	}
	
	
	public ChallengeEndEvent( double tunerTime, double wallTime, ParamConfiguration challenger, boolean newIncumbent, int numberOfRuns) {

		super(tunerTime, wallTime);
		this.challenger = challenger;
		this.newIncumbent = newIncumbent;
		this.numberOfRuns = numberOfRuns;
	}
	
	
	public ParamConfiguration getChallenger()
	{
		return challenger;
	}
	
	public boolean newIncumbent()
	{
		return newIncumbent;
	}
	
	public int getNumberOfRuns()
	{
		return numberOfRuns;
	}

	
	
	
}

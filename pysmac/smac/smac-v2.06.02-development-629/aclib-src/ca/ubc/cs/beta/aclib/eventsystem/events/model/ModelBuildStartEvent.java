package ca.ubc.cs.beta.aclib.eventsystem.events.model;


import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class ModelBuildStartEvent extends AbstractTimeEvent
{

	public ModelBuildStartEvent(TerminationCondition cond) {
		super(cond);
	}
	

}

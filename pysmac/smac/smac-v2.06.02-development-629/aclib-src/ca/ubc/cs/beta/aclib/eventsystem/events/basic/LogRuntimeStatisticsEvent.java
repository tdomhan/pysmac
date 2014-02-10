package ca.ubc.cs.beta.aclib.eventsystem.events.basic;

import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class LogRuntimeStatisticsEvent extends AbstractTimeEvent {

	public LogRuntimeStatisticsEvent(TerminationCondition cond) {
		super(cond);
	}

}

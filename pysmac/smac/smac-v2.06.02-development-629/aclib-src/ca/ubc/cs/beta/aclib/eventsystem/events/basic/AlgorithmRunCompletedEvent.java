package ca.ubc.cs.beta.aclib.eventsystem.events.basic;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class AlgorithmRunCompletedEvent extends AbstractTimeEvent {

	private final AlgorithmRun run;
	
	
	public AlgorithmRunCompletedEvent(TerminationCondition cond,AlgorithmRun run) {
		super(cond);
		this.run = run;
		
	}
	
	public AlgorithmRun getRun() {
		return run;
	}
	
	

}

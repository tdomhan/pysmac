package ca.ubc.cs.beta.aclib.eventsystem.events.ac;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class AutomaticConfigurationEnd extends AbstractTimeEvent {

	private final ParamConfiguration incumbent;
	private final double empiricalPerformance;

	/**
	 * Constructs an Automatic Configuration Event
	 * @param incumbent
	 * @param empiricalPerformance
	 * @param termCond 
	 */
	public AutomaticConfigurationEnd(TerminationCondition termCond,ParamConfiguration incumbent, double empiricalPerformance) {
		super(termCond);
		this.incumbent = incumbent;
		this.empiricalPerformance = empiricalPerformance;
	}

	

	/**
	 * Constructs an Automatic Configuration Event
	 * @param incumbent
	 * @param empiricalPerformance
	 * @param wallClockTime
	 * @param tunerTime
	 * @deprecated use the Termination Condition constructor
	 */
	public AutomaticConfigurationEnd(ParamConfiguration incumbent, double empiricalPerformance, long wallClockTime, double tunerTime) {
		super(tunerTime, wallClockTime);
		this.incumbent = incumbent;
		this.empiricalPerformance = empiricalPerformance;
	}

	public ParamConfiguration getIncumbent() {
		return incumbent;
	}
	
	public double getEmpiricalPerformance()
	{
		return empiricalPerformance;
	}

}

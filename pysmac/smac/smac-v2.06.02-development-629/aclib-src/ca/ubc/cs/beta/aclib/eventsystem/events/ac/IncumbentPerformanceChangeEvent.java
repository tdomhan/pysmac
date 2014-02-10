package ca.ubc.cs.beta.aclib.eventsystem.events.ac;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aclib.misc.cputime.CPUTime;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class IncumbentPerformanceChangeEvent extends AbstractTimeEvent {

	private final double acTime;
	private final ParamConfiguration incumbent;

	private final double empiricalPerformance;

	private final ParamConfiguration oldIncumbent;
	private final long runCount;
	
	public IncumbentPerformanceChangeEvent(double cpuTime, double walltime, double empiricalPerformance, ParamConfiguration incumbent , long runCount, ParamConfiguration oldIncumbent) 
	{
		super(cpuTime, walltime);
		this.empiricalPerformance = empiricalPerformance;
		
		this.incumbent = incumbent;
		this.acTime = CPUTime.getCPUTime();
		this.oldIncumbent = oldIncumbent;
		this.runCount = runCount;
		
	}

	public IncumbentPerformanceChangeEvent(TerminationCondition termCond, double empiricalPerformance, ParamConfiguration incumbent, long runCount, ParamConfiguration oldIncumbent ) 
	{
		super(termCond);
		this.empiricalPerformance = empiricalPerformance;
		
		this.incumbent = incumbent;
		this.acTime = CPUTime.getCPUTime();
		this.oldIncumbent = oldIncumbent;
		this.runCount = runCount;
	}

	public double getAutomaticConfiguratorCPUTime() {
		return acTime;
	}

	public ParamConfiguration getIncumbent() {
		return incumbent;
	}
	
	public double getEmpiricalPerformance() {
		return empiricalPerformance;
	}

	public boolean incumbentChanged()
	{
		return !incumbent.equals(oldIncumbent);
	}

	public long getIncumbentRunCount() {
		return runCount;
	}
	
	
	

}

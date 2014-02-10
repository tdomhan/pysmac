package ca.ubc.cs.beta.aclib.eventsystem.handlers;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;

public class ParamConfigurationIncumbentChangerOriginTracker implements	EventHandler<IncumbentPerformanceChangeEvent>
{
	

	private final ParamConfigurationOriginTracker configTracker;

	private final ThreadSafeRunHistory runHistory;

	private double cutoffTime;
	public ParamConfigurationIncumbentChangerOriginTracker(ParamConfigurationOriginTracker configTracker, ThreadSafeRunHistory runHistory, double cutoffTime)
	{
		this.configTracker = configTracker;
		this.runHistory = runHistory;
		this.cutoffTime = cutoffTime;
		
	}
	
	
	ParamConfiguration lastIncumbent;
	@Override
	public synchronized void handleEvent(IncumbentPerformanceChangeEvent event) 
	{
		
		
		if(event.incumbentChanged())
		{
			this.configTracker.addConfiguration(event.getIncumbent(), "Incumbent", "Performance="+event.getEmpiricalPerformance(), "Runs=" + event.getIncumbentRunCount());
			
			if(lastIncumbent != null)
			{
				runHistory.readLock();
				try {
				this.configTracker.addConfiguration(lastIncumbent, "Displaced Incumbent", "Performance=" + runHistory.getEmpiricalCost(lastIncumbent, runHistory.getInstancesRan(lastIncumbent), cutoffTime),"Runs=" + runHistory.getTotalNumRunsOfConfig(lastIncumbent));
				} finally
				{
					runHistory.releaseReadLock();
				}
			}
			
			lastIncumbent = event.getIncumbent();

			
		}
				
	}
}


package ca.ubc.cs.beta.aclib.eventsystem.handlers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.state.StateRestoredEvent;
import ca.ubc.cs.beta.aclib.misc.cputime.CPUTime;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;
import ca.ubc.cs.beta.aclib.termination.ValueMaxStatus;


/**
 * Logs some runtime information from the RunHistory and Termination Condition objects
 * <p>
 * <b>Events:</b> Requires IncumbentChangeEvent presently.
 * AlgorithmRunCompleted event is used as well (but not required)
 *   
 * and any other events that it receives will cause it to log the runhistory
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class LogRuntimeStatistics implements EventHandler<AutomaticConfiguratorEvent> 
{
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ThreadSafeRunHistory runHistory;
	private final TerminationCondition termCond;;
	
	private final AtomicReference<String> lastString = new AtomicReference<String>();
	
	private final AtomicReference<IncumbentPerformanceChangeEvent> lastICE = new AtomicReference<IncumbentPerformanceChangeEvent>();
	
	private final double cutoffTime;
	
	private final AtomicInteger logCount = new AtomicInteger(1);
	private final long msToWait; 
	
	private long lastMessage = Long.MIN_VALUE;
	
	private double sumOfRuntime = 0.0;
	private double sumOfWallclockTime = 0.0;
	
	private final AtomicBoolean noIceMessage = new AtomicBoolean(false);
	private final TargetAlgorithmEvaluator tae;
	public LogRuntimeStatistics(ThreadSafeRunHistory rh, TerminationCondition termCond, double cutoffTime, TargetAlgorithmEvaluator tae)
	{
		this.runHistory = rh;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.msToWait = 0;
		lastString.set("No Runtime Statistics Logged");
		this.tae = tae;
		
		
	}
	
	public LogRuntimeStatistics(ThreadSafeRunHistory rh, TerminationCondition termCond, double cutoffTime , long msToWait, TargetAlgorithmEvaluator tae)
	{
		this.runHistory = rh;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.msToWait = msToWait;
		lastString.set("No Runtime Statistics Logged");
		
		this.tae = tae;
	
	}

	
	@Override
	public synchronized void handleEvent(AutomaticConfiguratorEvent event) {
			
		
		if(event instanceof IncumbentPerformanceChangeEvent)
		{
			IncumbentPerformanceChangeEvent ice = (IncumbentPerformanceChangeEvent) event;
			
			
			if((this.lastICE.get() == null) || this.lastICE.get().getTunerTime() < ice.getTunerTime())
			{
				lastICE.set(ice);
				
			}
			return;
		} else if( event instanceof AlgorithmRunCompletedEvent )
		{
			this.sumOfWallclockTime += ((AlgorithmRunCompletedEvent) event).getRun().getWallclockExecutionTime();
			this.sumOfRuntime += ((AlgorithmRunCompletedEvent) event).getRun().getRuntime();
			
		} else if( event instanceof StateRestoredEvent)
		{
			this.logCount.set(((StateRestoredEvent) event).getModelsBuilt());
			
			for(AlgorithmRun run : ((StateRestoredEvent) event).getRunHistory().getAlgorithmRuns())
			{
				this.sumOfWallclockTime += run.getWallclockExecutionTime();
				this.sumOfRuntime += run.getRuntime();
			}
		} else
		{
			try {
				runHistory.readLock();
				String myLastLogMessage;
				if(this.lastICE.get() == null)
				{
					if(this.noIceMessage.get() == false)
					{
						log.debug("Runtime Statistics are Not Available because we haven't seen an Incumbent Performance Changed Event yet");	
					}
					return;
				}
				ParamConfiguration incumbent = this.lastICE.get().getIncumbent();
				
				Object[] arr = { logCount.get(),
						runHistory.getThetaIdx(incumbent) + " (" + incumbent +")",
						runHistory.getTotalNumRunsOfConfig(incumbent),
						runHistory.getInstancesRan(incumbent).size(),
						runHistory.getUniqueParamConfigurations().size(),
						runHistory.getEmpiricalCost(incumbent, runHistory.getUniqueInstancesRan(), this.cutoffTime),
						runHistory.getAlgorithmRuns().size(), 
						"N/A",
						"N/A" ,
						"N/A", //options.runtimeLimit - wallTime 
						termCond.getTunerTime(),
						"N/A", //options.scenarioConfig.tunerTimeout - tunerTime,
						runHistory.getTotalRunCost(),
						CPUTime.getCPUTime(),
						CPUTime.getUserTime() ,
						this.sumOfRuntime,
						this.sumOfWallclockTime,
						Runtime.getRuntime().maxMemory() / 1024.0 / 1024,
						Runtime.getRuntime().totalMemory() / 1024.0 / 1024,
						Runtime.getRuntime().freeMemory() / 1024.0 / 1024 };
				
				StringBuilder sb = new StringBuilder(" ");
				for(ValueMaxStatus vms : termCond.currentStatus())
				{
					sb.append(vms.getStatus());
				}
				
				myLastLogMessage = "*****Runtime Statistics*****" +
						"\n Count: " + arr[0]+
						"\n Incumbent ID: "+ arr[1]+
						"\n Number of Runs for Incumbent: " + arr[2] +
						"\n Number of Instances for Incumbent: " + arr[3]+
						"\n Number of Configurations Run: " + arr[4]+ 
						"\n Performance of the Incumbent: " + arr[5]+
						//"\n Total Number of runs performed: " + arr[6]+ 
						//"\n Last Iteration with a successful run: " + arr[7] + "\n" +
						"\n" + sb.toString().replaceAll("\n","\n ") + 
						//"\n Wallclock time: "+ arr[8] + " s" +
						//"\n Wallclock time remaining: "+ arr[9] +" s" +
						//"\n Configuration time budget used: "+ arr[10] +" s" +
						//"\n Configuration time budget remaining: "+ arr[11]+" s" +
						"Sum of Target Algorithm Execution Times (treating minimum value as 0.1): "+arr[12] +" s" + 
						"\n CPU time of Configurator: "+arr[13]+" s" +
						"\n User time of Configurator: "+arr[14]+" s" +
						"\n Outstanding Runs on Target Algorithm Evaluator: " + tae.getNumberOfOutstandingRuns() +
						"\n Outstanding Requests on TargetAlgorithmEvaluator: " + tae.getNumberOfOutstandingBatches() +  
						"\n Total Reported Algorithm Runtime: " + arr[15] + " s" + 
						"\n Sum of Measured Wallclock Runtime: " + arr[16] + " s" +
						"\n Max Memory: "+arr[17]+" MB" +
						"\n Total Java Memory: "+arr[18]+" MB" +
						"\n Free Java Memory: "+arr[19]+" MB";
				
			lastString.set(myLastLogMessage);
			} finally
			{
				runHistory.releaseReadLock();
			}
			
			
			if(msToWait + lastMessage > System.currentTimeMillis())
			{
				
				return;
			} else
			{
				logCount.incrementAndGet();
				lastMessage = System.currentTimeMillis();
				log.info(lastString.get());
			}
			
			
		}
		
		
	}

	public void logLastRuntimeStatistics() {
		log.info(lastString.get());
	}

	


}

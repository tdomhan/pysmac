package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

@ThreadSafe
/***
 * If runs ignore there cutoff time will eventually kill them
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	
	
	private final double scalingFactor;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * Stores runs we have already killed in a weak map so that they can be garbage collected if need be.
	 * The synchronization here is for memory visibility only, it doesn't
	 *
	 */
	
	private final Set<RunConfig> killedRuns = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<RunConfig, Boolean>()));  
	
	public KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, double scalingFactor) {
		super(tae);
		if(scalingFactor <= 1.0)
		{
			throw new ParameterException("Scaling Factor for killing cannot be less than or equal to 1.0");
		}
		
		if(scalingFactor < 2.0)
		{
			log.warn("Scaling factors less than 2.0 are STRONGLY discouraged, as the runtime observations we make are only very approximate.");
		}
		
		this.scalingFactor = scalingFactor;
	}

	@Override
	public final List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return tae.evaluateRun(runConfigs, new KillingTargetAlgorithmEvaluatorRunObserver(obs));
	}
	
	
	
	@Override
	public final void evaluateRunsAsync(final List<RunConfig> runConfigs, final TargetAlgorithmEvaluatorCallback oHandler, final TargetAlgorithmEvaluatorRunObserver obs) {
		tae.evaluateRunsAsync(runConfigs, oHandler, new KillingTargetAlgorithmEvaluatorRunObserver(obs));

	}
	

	private class KillingTargetAlgorithmEvaluatorRunObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private TargetAlgorithmEvaluatorRunObserver obs;
		KillingTargetAlgorithmEvaluatorRunObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
		}
		
		@Override
		public void currentStatus(List<? extends KillableAlgorithmRun> runs) 
		{
			
			for(KillableAlgorithmRun run : runs)
			{
				
				if(run.getRunResult().equals(RunResult.RUNNING))
				{
					
					if(run.getRunConfig().getCutoffTime() * scalingFactor < run.getRuntime())
					{
						
						if(!killedRuns.contains(run.getRunConfig()))
						{
							
							Object[] args = { run.getRunConfig() ,run.getRuntime(), scalingFactor, run.getRunConfig().getCutoffTime()};
							log.warn("Killed run {} at {} for exceeding {} times its cutoff time of {} (secs)", args);
						
							run.kill();
						}
				}
			}
			if(obs != null)
			{
				obs.currentStatus(runs);
			}
		}
		
		}
	}
}


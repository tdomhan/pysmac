package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug;



import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Target Algorithm Evaluator Decorator that checks if duplicate runs are being submitted
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class CheckForDuplicateRunConfigDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	Logger log = LoggerFactory.getLogger(getClass());
	private boolean throwException;
	
	
	public CheckForDuplicateRunConfigDecorator(
			TargetAlgorithmEvaluator tae, boolean throwException) {
		super(tae);
		this.throwException  = throwException;
	}


	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		checkRunConfigs(runConfigs);
		return tae.evaluateRun(runConfigs,obs);
	}


	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		checkRunConfigs(runConfigs);
		tae.evaluateRunsAsync(runConfigs, handler, obs);
		
	}
	
	public void checkRunConfigs(List<RunConfig> runConfigs)
	{
		Set<RunConfig> rcs = new HashSet<RunConfig>();
		
		rcs.addAll(runConfigs);
		
		if(rcs.size() != runConfigs.size())
		{
			log.error("Duplicate Run Configurations Requested this is almost certainly a bug");
			log.error("Duplicate Run Configs Follow:");
			for(RunConfig rc : findDuplicates(runConfigs))
			{
				log.error("\tDuplicate Run Config: {}", rc);
			}
			
			
			log.error("All Run Configs follow:");
			for(RunConfig rc : runConfigs)
			{
				log.error("\tRun Config: {} ", rc);
			}


			if(throwException)
			{
				throw new IllegalStateException("Duplicate Run Configurations cannot be part of the same call of evaluateRun()/evaluateRunAsync()");
			}
		}	

	}
	
	private Set<RunConfig> findDuplicates(List<RunConfig> listContainingDuplicates)
	{ 
	  final Set<RunConfig> setToReturn = new HashSet<RunConfig>(); 
	  final Set<RunConfig> set1 = new HashSet<RunConfig>();

	  for (RunConfig yourInt : listContainingDuplicates)
	  {
	   if (!set1.add(yourInt))
	   {
	    setToReturn.add(yourInt);
	   }
	  }
	  return setToReturn;
	}

	
	
}

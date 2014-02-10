package ca.ubc.cs.beta.aclib.algorithmrunner;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;

/**
 * Factory that creates various Algorithm Runners for things that request it.
 * 
 * </b>NOTE:</b> This factory is probably unnecessary, originally it was meant to do more, but things got side tracked.
 * This class should probably be refactored out and merged in with just the CLI TAE
 * 
 * 
 * 
 * @see CommandLineTargetAlgorithmEvaluator
 * @author sjr
 *
 */
public class AutomaticConfiguratorFactory {

	
	private static int maxThreads = Runtime.getRuntime().availableProcessors();

	
	private static Logger log = LoggerFactory.getLogger(AutomaticConfiguratorFactory.class);
	/**
	 * Returns an AlgorithmRunner that executes all requests serially
	 * @param execConfig		execution configuration of the target algorithm
	 * @param runConfigs		run configurations to execute
	 * @return	algorithmrunner which will run it
	 */
	public static AlgorithmRunner getSingleThreadedAlgorithmRunner(AlgorithmExecutionConfig execConfig, List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs)
	{
		return new SingleThreadedAlgorithmRunner(execConfig, runConfigs,obs, options,executionIDs);
	}
	
	/**
	 * Returns an AlgorithmRunner that executes as many requests concurrently as there are cores
	 * @param execConfig		execution configuration of the target algorithm
	 * @param runConfigs		run configurations to execute
	 * @return	algorithmrunner which will run it
	 */	
	public static AlgorithmRunner getConcurrentAlgorithmRunner(AlgorithmExecutionConfig execConfig, List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs)
	{
		if(runConfigs.size() == 1)
		{
			return getSingleThreadedAlgorithmRunner(execConfig, runConfigs,obs, options,executionIDs);
		}
		
		if(options.cores > maxThreads)
		{
			log.warn("Number of cores requested is seemingly greater than the number of available cores. This may affect runtime measurements");
		}
		
		return getConcurrentAlgorithmRunner(execConfig, runConfigs, options.cores, obs, options,executionIDs);
	}
	
	/**
	 * Returns an AlgorithmRunner that executse up to nThreads concurrently
	 * @param execConfig		execution configuration of the target algorithm
	 * @param runConfigs		run configurations to execute
	 * @param nThreads			number of concurrent executions to allow
	 * @return	algorithmrunner which will run it
	 */
	public static AlgorithmRunner getConcurrentAlgorithmRunner(AlgorithmExecutionConfig execConfig, List<RunConfig> runConfigs, int nThreads, TargetAlgorithmEvaluatorRunObserver obs, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs)
	{
		log.debug("Concurrent Algorithm Runner created allowing {} threads", nThreads);
		return new ConcurrentAlgorithmRunner(execConfig, runConfigs, nThreads, obs, options,executionIDs);
	}

}

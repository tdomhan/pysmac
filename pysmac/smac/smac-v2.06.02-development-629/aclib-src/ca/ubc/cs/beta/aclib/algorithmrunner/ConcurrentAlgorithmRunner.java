package ca.ubc.cs.beta.aclib.algorithmrunner;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Processes Algorithm Run Requests concurrently 
 * 
 * @author seramage
 * 
 */

class ConcurrentAlgorithmRunner extends AbstractAlgorithmRunner {

	
	private int numberOfConcurrentExecutions;
	private static final Logger log = LoggerFactory.getLogger(ConcurrentAlgorithmRunner.class);
	
	/**
	 * Default Constructor 
	 * @param execConfig	execution configuration of target algorithm
	 * @param runConfigs	run configurations to execute
	 * @param numberOfConcurrentExecutions	number of concurrent executions allowed
	 * @param obs 
	 * @param executionIDs 
	 */
	public ConcurrentAlgorithmRunner(AlgorithmExecutionConfig execConfig,
			List<RunConfig> runConfigs, int numberOfConcurrentExecutions, TargetAlgorithmEvaluatorRunObserver obs, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs) {
		super(execConfig, runConfigs, obs, options,executionIDs);
		this.numberOfConcurrentExecutions = numberOfConcurrentExecutions;
	}

	@Override
	public synchronized List<AlgorithmRun> run() {
		
		log.debug("Creating Thread Pool Supporting " + numberOfConcurrentExecutions);
		
		ExecutorService p = Executors.newFixedThreadPool(numberOfConcurrentExecutions, new SequentiallyNamedThreadFactory("Command Line Target Algorithm Evaluator"));
		/*
		 * Runs all algorithms in the thread pool
		 * Tells it to shutdown
		 * Waits for it to shutdown
		 * 
		 */
		try {
			
	
			
			try {
				p.invokeAll(runs);
				for(AlgorithmRun run : runs)
				{
					if (run.getRunResult().equals(RunResult.ABORT))
					{
						throw new TargetAlgorithmAbortException(run);
					}
				}
			} catch (InterruptedException e) {
				//TODO We probably need to actually abort properly
				//We can't just let something else do it, I think.
				//Additionally runs are in an invalid state at this point
				Thread.currentThread().interrupt();
			}
		} finally
		{
			p.shutdown();
		}
		return runs;
	}

}

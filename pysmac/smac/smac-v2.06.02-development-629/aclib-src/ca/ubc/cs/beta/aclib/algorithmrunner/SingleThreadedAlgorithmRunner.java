package ca.ubc.cs.beta.aclib.algorithmrunner;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

class SingleThreadedAlgorithmRunner extends AbstractAlgorithmRunner
{

	/**
	 * Default Constructor 
	 * @param execConfig	execution configuration of target algorithm
	 * @param runConfigs	run configurations to execute
	 * @param obs 
	 */
	public SingleThreadedAlgorithmRunner(AlgorithmExecutionConfig execConfig,
			List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs) {
		super(execConfig, runConfigs,obs, options, executionIDs);
		
	}

	@Override
	public List<AlgorithmRun> run() 	
	{
		for(AlgorithmRun run : runs)
		{
			run.run();
			
			if(run.getRunResult().equals(RunResult.ABORT))
			{
				throw new TargetAlgorithmAbortException(run);
			}
		}
			
		return runs;
	
		
	}

	
}

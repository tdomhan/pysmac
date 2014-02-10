package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.general;

import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * Interface that needs to be implemented / subtyped to provide context
 * <p>
 * The point of this interface is it allows you to save along with the set of runs
 * other information you need to process them correctly without having to save them elsewhere.
 * <p>
 * A default implementation that provides no additional information is: 
 * {@link ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext}
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface TargetAlgorithmEvaluatorQueueResultContext {

	public List<AlgorithmRun> getAlgorithmRuns();
	
	public void setAlgorithmRuns(List<AlgorithmRun> runs);
	
	public List<RunConfig> getRunConfigs();
	
	public void setRunConfigs(List<RunConfig> runConfigs);
	
	public void setRuntimeException(RuntimeException t);
	
	public RuntimeException getRuntimeException();
}

package ca.ubc.cs.beta.aclib.algorithmrunner;

import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

/**
 * Interface for objects that can handle running RunConfigs and AlgorithmExecutionConfigs
 * 
 * Use the AutomaticConfiguratorFactory to get your Runner
 * @author seramage
 * 
 */
public interface AlgorithmRunner {

	/**
	 * Runs the Algorithm, returning the AlgorithmRuns in the same order as they were specified
	 * @return list of <code>AlgorithmRun</code>, in order they were specified
	 */
	public List<AlgorithmRun> run();
	
	public void shutdownThreadPool();

}
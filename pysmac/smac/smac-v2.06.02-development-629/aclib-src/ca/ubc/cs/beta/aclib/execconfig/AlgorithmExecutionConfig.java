package ca.ubc.cs.beta.aclib.execconfig;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;

/**
 * Immutable Object contains all the information related to executing a target algorithm run
 * @author seramage
 *
 */
public class AlgorithmExecutionConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1009816326679512474L;
	
	private final String algorithmExecutable;
	private final String algorithmExecutionDirectory;
	private final ParamConfigurationSpace paramFile;
	private final boolean executeOnCluster;
	private final boolean deterministicAlgorithm;

	private final double cutoffTime; 

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	
	public AlgorithmExecutionConfig(String algorithmExecutable, String algorithmExecutionDirectory,
			ParamConfigurationSpace paramFile, boolean executeOnCluster, boolean deterministicAlgorithm, double cutoffTime) {
		this.algorithmExecutable = algorithmExecutable;
		this.algorithmExecutionDirectory = algorithmExecutionDirectory;
		this.paramFile = paramFile;
		this.executeOnCluster = executeOnCluster;
		this.deterministicAlgorithm = deterministicAlgorithm;
		if(cutoffTime < 0)
		{
			throw new IllegalArgumentException("Cutoff time must be greater than zero");
		}
		
		if(cutoffTime == 0)
		{
			log.warn("Cutoff time is greater than zero");
		}
		this.cutoffTime = cutoffTime;
		

	}

	public String getAlgorithmExecutable() {
		return algorithmExecutable;
	}

	public String getAlgorithmExecutionDirectory() {
		return algorithmExecutionDirectory;
	}

	public ParamConfigurationSpace getParamFile() {
		return paramFile;
	}

	@Deprecated
	/**
	 * @deprecated this really never did anything and will be removed at some point
	 */
	public boolean isExecuteOnCluster() {
		return executeOnCluster;
	}
	
	public boolean isDeterministicAlgorithm()
	{
		return deterministicAlgorithm;
	}
	
	
	public int hashCode()
	{
		return algorithmExecutable.hashCode() ^ algorithmExecutionDirectory.hashCode() ^ paramFile.hashCode() ^ (executeOnCluster ? 0 : 1) ^ (deterministicAlgorithm ? 0 : 1);
	}
	
	public String toString()
	{
		return "algoExec:" + algorithmExecutable + "\nAlgorithmExecutionDirectory:" + algorithmExecutionDirectory + "\n"+paramFile + "\n Cluster:"+executeOnCluster+ "\nDetermininstic:" + deterministicAlgorithm;
	}
	
	public boolean equals(Object o)
	{ 
		if(this == o) return true;
		if (o instanceof AlgorithmExecutionConfig)
		{
			AlgorithmExecutionConfig co = (AlgorithmExecutionConfig) o;
			return (co.algorithmExecutable.equals(algorithmExecutable) && co.algorithmExecutionDirectory.equals(algorithmExecutionDirectory) && (co.executeOnCluster == executeOnCluster) && co.paramFile.equals(paramFile)) && co.deterministicAlgorithm == deterministicAlgorithm ;
		} 
		return false;
	}
	
	/**
	 * Returns the maximum cutoff time
	 * @return maximum cutoff time for the algorithm
	 */
	public double getAlgorithmCutoffTime() {
		return cutoffTime;
	}
	
	
	public final static String MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX = "Who am I, Alan Turing?...also from X-Men?";
}

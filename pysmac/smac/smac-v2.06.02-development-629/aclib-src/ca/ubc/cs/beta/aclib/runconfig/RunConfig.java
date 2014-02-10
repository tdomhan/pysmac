package ca.ubc.cs.beta.aclib.runconfig;

import java.io.Serializable;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;

/**
 * Immutable class that contains all the information necessary for a target algorithm run.
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class RunConfig implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7749039021874017859L;
	private final ProblemInstanceSeedPair pisp;
	private final double cutoffTime;
	private final ParamConfiguration params;
	private final boolean cutoffLessThanMax;

	/**
	 * 
	 * @param pisp 			problem instance and seed that we will run against
	 * @param cutoffTime 	double representing the amount of time to execute for (in seconds)
	 * @param config 		paramconfiguration of the target algorithm
	 */
	public RunConfig(ProblemInstanceSeedPair pisp, double cutoffTime, ParamConfiguration config)
	{
		if(pisp == null)
		{
			throw new IllegalArgumentException("AlgorithmInstanceSeedPair cannot be null");
		}
		
		if(config == null)
		{
			throw new IllegalArgumentException("Params cannot be null");
		}
		
		if(cutoffTime  < 0)
		{
			throw new IllegalArgumentException("Cutoff time must be non-negative positive");
		}
		this.pisp = pisp;
		this.cutoffTime = cutoffTime;
		this.params = config;
		this.cutoffLessThanMax = false;

	}
	
	/**
	 * Default Constructor
	 * @param pisp 					problem instance and seed that we will run against
	 * @param cutoffTime 			double representing the amount of time to execute for (in seconds)
	 * @param params 				paramconfiguration of the target algorithm
	 * @param cutoffLessThanMax    boolean representing whether the cutoffTime is less than the maximum possible (in other words whether this run was capped)
	 */
	public RunConfig(ProblemInstanceSeedPair pisp, double cutoffTime, ParamConfiguration params, boolean cutoffLessThanMax)
	{
		if(pisp == null)
		{
			throw new IllegalArgumentException("ProblemInstanceSeedPair Name cannot be null");
		}
		
		if(cutoffTime  < 0)
		{
			throw new IllegalArgumentException("Cutoff time must be non-negative positive");
		}

		if(params == null)
		{
			throw new IllegalArgumentException("ParamString cannot be null");
		}
		
		this.pisp = pisp;
		this.cutoffTime = cutoffTime;
		this.params = params;
		this.cutoffLessThanMax = cutoffLessThanMax;
	}
	


	/**
	 * 
	 * @return probleminstanceseedpair for the run
	 */
	public ProblemInstanceSeedPair getProblemInstanceSeedPair()
	{
		return pisp;
	}

	/**
	 * 
	 * @return cuttoff time of the run
	 */
	public double getCutoffTime() {
		return cutoffTime;
	}

	/**
	 * Returns a COPY of the Param Configuration to be run
	 * @return a copy of the param configuration to be run
	 */
	public ParamConfiguration getParamConfiguration()
	{
		return new ParamConfiguration(params);
	}
	
	/**
	 * @return <code>true</code> if this run has a cutoff less than the cutoff time less than kappaMax.
	 */
	public boolean hasCutoffLessThanMax()
	{
		return cutoffLessThanMax;
	}
	
	/**
	 * @return <code>true</code> if the two runconfigs have the same cutoffTime, probleminstanceseedpair, and param configuration, <code>false</code> otherwise
	 */
	@Override
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if (o instanceof RunConfig)
		{
			RunConfig oar = (RunConfig) o;
			return (pisp.equals(oar.pisp)) && cutoffTime == oar.cutoffTime && params.equals(oar.params);
		} else
		{
			return false;
		}
	} 
	
	
	@Override
	public int hashCode()
	{
		/*
		 * Due to adaptive Capping and floating point issues, we don't consider the cutofftime as part of the hashcode.
		 * Theoretically this may cause certain performance issues in hash based collections
		 * however it is hoped that the number of re-runs with increasing cap times is small.
		 */
	
		return (int) ( (pisp.hashCode())^ params.hashCode());
	}
	
	@Override
	public String toString()
	{
		int instID = this.getProblemInstanceSeedPair().getInstance().getInstanceID();
		long seed = this.getProblemInstanceSeedPair().getSeed();
		String confID = this.params.getFriendlyIDHex();
		StringBuilder sb = new StringBuilder();
		sb.append("<Instance:" +instID + ", Seed:" + seed + ", Config:" + confID+", Kappa:" + cutoffTime+">");
		return sb.toString();
		
	}
	
	/**
	 * Returns a user friendly representation of this run object
	 * @return friendly representation of this object
	 */
	public String getFriendlyRunInfo()
	{
		int instID = this.getProblemInstanceSeedPair().getInstance().getInstanceID();
		long seed = this.getProblemInstanceSeedPair().getSeed();
		String confID = this.params.getFriendlyIDHex();
		return "Run for Instance (" + instID + ") Config (" +confID + ") Seed: (" + seed +")";   
	}
	
	
	

}

package ca.ubc.cs.beta.aclib.probleminstance;

import java.io.Serializable;

/**
 * Immutable Class that represents an Algorithm Instance and Seed Pair
 * @author seramage
 *
 */
public class ProblemInstanceSeedPair implements Comparable<ProblemInstanceSeedPair>,Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7686639875384341346L;
	private final ProblemInstance ai;
	private final long seed;
	
	public ProblemInstanceSeedPair(ProblemInstance ai, long seed)
	{
		if(ai == null)
		{
			throw new IllegalArgumentException("Algorithm Instance cannot be null");
		}
		this.ai = ai;
		this.seed = seed;
	}
	
	public ProblemInstance getInstance()
	{
		return ai;
	}
	 
	public long getSeed()
	{
		return seed;
	}
	
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if(o instanceof ProblemInstanceSeedPair)
		{
			ProblemInstanceSeedPair aisp = (ProblemInstanceSeedPair) o;
			return ((aisp.seed == seed) && ai.equals(aisp.ai)); 
		}
		return false;
	}
	
	public int hashCode()
	{
		
		return (int) ((seed >>> 32) ^ ((int) seed ) ^ ai.hashCode()); 
	}
	
	public String toString()
	{
		return "<Instance:" + ai.getInstanceID() + ", Seed:" + seed + ">";
		//return ai.toString() + "\nSeed:" + seed;
	}

	@Override
	public int compareTo(ProblemInstanceSeedPair o) {
		int idDiff = ai.getInstanceID() - o.ai.getInstanceID();
		if(idDiff != 0)
		{
		       return idDiff;
		}
		
		long seedDiff =  seed - o.seed;
		
		if(seedDiff < 0)
		{
			return -1;
		} else if(seedDiff > 0)
		{
			return +1;
		}
		return 0;
		
		
		
	}

}

/*package ca.ubc.cs.beta.aclib.runconfig;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DeveloperMadeABooBooException;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;

*//**
 * Subtype of RunConfig that supports additional runs in certain circumstances.
 * <p>
 * <b>WARNING</b> This has a very specific purpose currently. You should be very careful
 * when using this class as in general the equality and hashCode().
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *//*
public class VersionedRunConfig extends RunConfig {

	*//**
	 * 
	 *//*
	private static final long serialVersionUID = -691370122766133238L;
	
	private final int version;
	
	public VersionedRunConfig(ProblemInstanceSeedPair pisp, double cutoffTime,
			ParamConfiguration params, boolean cutoffLessThanMax, int version) {
		super(pisp, cutoffTime, params, cutoffLessThanMax);
		this.version = version;
	}
	public VersionedRunConfig(ProblemInstanceSeedPair pisp, double cutoffTime,
			ParamConfiguration config, int version) {
		super(pisp, cutoffTime, config);
		this.version = version;
	}
	
	public int getVersion()
	{
		return version;
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof VersionedRunConfig)
		{
			VersionedRunConfig oar = (VersionedRunConfig) o;
			
			boolean equals = equals(o);
			return (equals && version == oar.version);
		} else if (o instanceof RunConfig) 
		{
			throw new DeveloperMadeABooBooException("You are mixing VersionRunConfig objects with non version runconfig objects. The equality operation is no longer symmetric. See Item 8 in Effective Java.");
		}
		return false;
		
	}
	
	

}
*/
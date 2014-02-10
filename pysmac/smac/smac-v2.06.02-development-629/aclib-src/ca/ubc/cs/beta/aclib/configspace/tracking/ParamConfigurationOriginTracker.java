package ca.ubc.cs.beta.aclib.configspace.tracking;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;

import net.jcip.annotations.ThreadSafe;

/**
 * Allows Point Selectors and other processes to track who created what configuration
 * 
 * Iteration order is insertion order.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public interface ParamConfigurationOriginTracker extends Iterable<ParamConfiguration> {

	
	public void addConfiguration(ParamConfiguration config, String origin, String... addlData);
		
	public Map<String, String> getOrigins(ParamConfiguration config);
	
	
	public Long getCreationTime(ParamConfiguration config);
	
	public Set<String> getOriginNames();
	
	@Override
	public Iterator<ParamConfiguration> iterator();
	
	
	public int size();
	
	
	public int getGenerationCount(ParamConfiguration config);
	
}

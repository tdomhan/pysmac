package ca.ubc.cs.beta.aclib.configspace.tracking;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
public class RealParamConfigurationOriginTracker implements ParamConfigurationOriginTracker {

	
	private final ConcurrentHashMap<ParamConfiguration, ConcurrentHashMap<String, String>> originTracker = new ConcurrentHashMap<ParamConfiguration, ConcurrentHashMap<String,String>>();
	private final ConcurrentHashMap<ParamConfiguration, Long> firstGenerated = new ConcurrentHashMap<ParamConfiguration, Long>();
	private final ConcurrentHashMap<ParamConfiguration, AtomicInteger> generationCount = new ConcurrentHashMap<ParamConfiguration, AtomicInteger>();
	
	/**
	 * We use this so we can get an iterator that gives us insertion order
	 */
	private final LinkedBlockingQueue<ParamConfiguration> insertionOrderTracker = new LinkedBlockingQueue<ParamConfiguration>();
	
	
	private final Set<String> originNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	/**
	 * Basically used as a set to store configs that already exist, this is used to prevent us from hitting the same object twice
	 * in the insertion order. We don't have a putIfAbsent operation if we wrap as a set
	 */
	private final ConcurrentHashMap<ParamConfiguration, Object> originConfigs =new ConcurrentHashMap<ParamConfiguration, Object>();
	
	private final Object dummyObject = new Object();
	
	@Override
	public void addConfiguration(ParamConfiguration config, String origin, String... addlData)
	{
		
			originTracker.putIfAbsent(config, new ConcurrentHashMap<String, String>());
			StringBuilder sb = new StringBuilder("");
			
			generationCount.putIfAbsent(config, new AtomicInteger(0));
			generationCount.get(config).incrementAndGet();
			for(String addlDatum : addlData)
			{
				sb.append(addlDatum).append("; ");
			}
		
		
			originTracker.get(config).put(origin, sb.toString());
			
			firstGenerated.putIfAbsent(config, System.currentTimeMillis());
			
			
			Object alreadyExisted = originConfigs.putIfAbsent(config, dummyObject);
			if(alreadyExisted == null)
			{
				insertionOrderTracker.add(config);
			}
			
			originNames.add(origin);
	}

	@Override
	public Map<String, String> getOrigins(ParamConfiguration config)
	{
		return originTracker.get(config);
	}
	
	@Override
	public Long getCreationTime(ParamConfiguration config)
	{
		return firstGenerated.get(config);
	}
	
	@Override
	public Set<String> getOriginNames()
	{
		return Collections.unmodifiableSet(originNames);
	}
	
	@Override
	public Iterator<ParamConfiguration> iterator()
	{
		return insertionOrderTracker.iterator();
	}
	
	@Override
	public int size()
	{
		//DO NOT USE insertionOrderTracker to generate the size() because it is an O(n) operation there
		return originTracker.size();
	}
	
	@Override
	public int getGenerationCount(ParamConfiguration config)
	{
		AtomicInteger genCount = generationCount.get(config);
		if(genCount == null ) return 0;
		return genCount.get();
	}
}

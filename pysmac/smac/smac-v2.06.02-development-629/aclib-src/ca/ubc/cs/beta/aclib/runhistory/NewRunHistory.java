package ca.ubc.cs.beta.aclib.runhistory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.objectives.OverallObjective;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;

/**
 * 
 * @author seramage
 *
 */
@SuppressWarnings("unused")
@NotThreadSafe
public class NewRunHistory implements RunHistory {


	/**
	 * Objective function that allows us to aggregate various instance / seed pairs to 
	 * give us a value for the instance
	 */
	private final OverallObjective perInstanceObjectiveFunction;
	
	/**
	 * Objective function that allows us to aggeregate various instances (aggregated by the perInstanceObjectiveFunction), 
	 * to determine a cost for the set of instances.
	 */
	private final OverallObjective aggregateInstanceObjectiveFunction;
	
	/**
	 * Objective function that determines the response value from a run.
	 */
	private final RunObjective runObj;
	
	/**
	 * Current iteration we are on
	 */
	private int iteration = 0;

	/**
	 * Stores a list of Parameter Configurations along with there associted thetaIdx
	 */
	private final KeyObjectManager<ParamConfiguration> paramConfigurationList = new KeyObjectManager<ParamConfiguration>();
	
	/**
	 * Stores a list of RunData
	 */
	private final List<RunData> runHistoryList = new ArrayList<RunData>();
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Stores the sum of all the runtimes
	 */
	private double totalRuntimeSum = 0;
	
	/**
	 * Stores for each configuration a mapping of instances to a map of seeds => response values
	 * 
	 * We store the Seeds in a LinkedHashMap because the order of them matters as far as determining which seed to pick to run
	 * as far as Matlab synchronizing goes. Otherwise it could be a regular map
	 */
	private final Map<ParamConfiguration, Map<ProblemInstance, LinkedHashMap<Long, Double>>> configToPerformanceMap =
			new HashMap<ParamConfiguration, Map<ProblemInstance, LinkedHashMap<Long, Double>>>();
	
	/**
	 * Stores for each instance the list of seeds used 
	 */
	private final HashMap<ProblemInstance, List<Long>> seedsUsedByInstance = new HashMap<ProblemInstance, List<Long>>();
	
	/**
	 * Stores the number of times a config has been run
	 */
	private final LinkedHashMap<ParamConfiguration, Integer> configToNumRunsMap = new LinkedHashMap<ParamConfiguration, Integer>();
	
	
	/**
	 * Stores a list of Instance Seed Pairs whose runs were capped.
	 */
	private final HashMap<ParamConfiguration, Set<ProblemInstanceSeedPair>> cappedRuns = new HashMap<ParamConfiguration, Set<ProblemInstanceSeedPair>>(); 
	
	/**
	 * Stores the set of instances we have run
	 */
	private Set<ProblemInstance> instancesRanSet = new HashSet<ProblemInstance>();
	
	
	private final HashMap<ParamConfiguration, List<AlgorithmRun>> configToRunMap = new HashMap<ParamConfiguration, List<AlgorithmRun>>();
	
	private static final DecimalFormat format = new DecimalFormat("#######.####");
	
	/**
	 * Creates NewRunHistory object
	 * @param intraInstanceObjective	intraInstanceObjective to use when calculating costs
	 * @param interInstanceObjective	interInstanceObjective to use when calculating costs
	 * @param runObj					run objective to use 
	 */
	public NewRunHistory( OverallObjective intraInstanceObjective,  OverallObjective interInstanceObjective, RunObjective runObj)
	{
		this.perInstanceObjectiveFunction = intraInstanceObjective;
		this.aggregateInstanceObjectiveFunction = interInstanceObjective;
		this.runObj = runObj;
	
	}
	
	@Override
	public void append(AlgorithmRun run) throws DuplicateRunException{

		log.trace("Appending Run {}",run);
		
		if(run.getRunResult().equals(RunResult.RUNNING))
		{
			throw new IllegalArgumentException("Runs with Run Result RUNNING cannot be saved to a RunHistory object");
		}
		ParamConfiguration config = run.getRunConfig().getParamConfiguration();
		ProblemInstanceSeedPair pisp = run.getRunConfig().getProblemInstanceSeedPair();
		ProblemInstance pi = pisp.getInstance();
		long seed = run.getResultSeed();
		
		Double runResult = runObj.getObjective(run);
		
		
		
		/**
		 * Add run data to the list of seeds used by Instance
		 */
		List<Long> instanceSeedList = seedsUsedByInstance.get(pi);
		if(instanceSeedList == null)
		{ //Initialize List if non existant
			instanceSeedList = new LinkedList<Long>();
			seedsUsedByInstance.put(pi,instanceSeedList);
		}
		instanceSeedList.add(seed);
		
		
	
		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceToPerformanceMap = configToPerformanceMap.get(config);
		if(instanceToPerformanceMap == null)
		{ //Initialize Map if non-existant
			instanceToPerformanceMap = new HashMap<ProblemInstance, LinkedHashMap<Long,Double>>();
			configToPerformanceMap.put(config,instanceToPerformanceMap);
		}
		
		LinkedHashMap<Long, Double> seedToPerformanceMap = instanceToPerformanceMap.get(pi);
		if(seedToPerformanceMap == null)
		{ //Initialize Map if non-existant
			seedToPerformanceMap = new LinkedHashMap<Long, Double>();
			instanceToPerformanceMap.put(pi, seedToPerformanceMap);
		}
		
		Double dOldValue = seedToPerformanceMap.put(seed,runResult);
		
		if(dOldValue != null)
		{
			//If the value already existed then either
			//we have a duplicate run OR the previous run was capped
			
			Set<ProblemInstanceSeedPair> cappedRunsForConfig = cappedRuns.get(config);
			
			
			
			if((cappedRunsForConfig != null) && cappedRunsForConfig.contains(pisp))
			{
				//We remove it now and will re-add it if this current run was capped
				cappedRunsForConfig.remove(pisp); 
			} else
			{
			
			
			
				
				AlgorithmRun matchingRun = null;
				for(AlgorithmRun algoRun : this.getAlgorithmRunData(config))
				{
					if(algoRun.getRunConfig().getProblemInstanceSeedPair().equals(run.getRunConfig().getProblemInstanceSeedPair()))
					{
						matchingRun = algoRun;
					}
				}
				
				Object[] args = {matchingRun, run, config, pi,dOldValue};
				
				
				log.error("RunHistory already contains a run with identical config, instance and seed \n Original Run:{}\nRun:{}\nConfig:{}\nInstance:{}\nPrevious Performance:{}", args);
				throw new DuplicateRunException("Duplicate Run Detected", run);
			}
			
		}
		
		if(this.configToRunMap.get(config) == null)
		{
			this.configToRunMap.put(config, new ArrayList<AlgorithmRun>());
		}
		
		this.configToRunMap.get(config).add(run);
		totalRuntimeSum += Math.max(0.1, run.getRuntime());
		
		/*
		 * Add data to the run List
		 */
		int thetaIdx = paramConfigurationList.getOrCreateKey(config);
		
	
		
		int instanceIdx = pi.getInstanceID();
		RunResult result = run.getRunResult();
		boolean cappedRun = (result.equals(RunResult.TIMEOUT) && run.getRunConfig().hasCutoffLessThanMax() || result.equals(RunResult.KILLED));
		runHistoryList.add(new RunData(iteration, thetaIdx, instanceIdx, run,runResult, cappedRun));
		
		
		/*
		 * Increment the config run counter
		 */
		if(configToNumRunsMap.get(config) == null)
		{
			configToNumRunsMap.put(config, Integer.valueOf(1));
		} else
		{
			configToNumRunsMap.put(config, configToNumRunsMap.get(config) +1);
		}
		
		/*
		 * Add Instance to the set of instances ran 
		 */
		instancesRanSet.add(pi);
		
		/*
		 * Add to the capped runs set
		 */
		if(cappedRun)
		{
			if(!cappedRuns.containsKey(config))
			{
				cappedRuns.put(config, new LinkedHashSet<ProblemInstanceSeedPair>());
			}
				
			cappedRuns.get(config).add(pisp);
		}
		
		Object[] args = {iteration, paramConfigurationList.getKey(config), pi.getInstanceID(), pisp.getSeed(), format.format(run.getRunConfig().getCutoffTime())};
		
		
		
		//
	
		
	}

	
	
	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime)
	{
		Map<ProblemInstance, Map<Long, Double>> foo = Collections.emptyMap();
		return getEmpiricalCost(config, instanceSet, cutoffTime, foo);
	}
	
	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime, double minimumResponseValue)
	{
		Map<ProblemInstance, Map<Long, Double>> foo = Collections.emptyMap();
		return getEmpiricalCost(config, instanceSet, cutoffTime, foo, minimumResponseValue);
	}
	
	
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime, Map<ProblemInstance, Map<Long,Double>> hallucinatedValues)
	{
		return getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, Double.NEGATIVE_INFINITY);
	}
	
	@Override
	public double getEmpiricalCost(ParamConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime, Map<ProblemInstance, Map<Long,Double>> hallucinatedValues, double minimumResponseValue)
	{
		if (!configToPerformanceMap.containsKey(config) && hallucinatedValues.isEmpty()){
			return Double.MAX_VALUE;
		}
		ArrayList<Double> instanceCosts = new ArrayList<Double>();
		
		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = configToPerformanceMap.get(config);

		if(instanceSeedToPerformanceMap == null) 
		{
			instanceSeedToPerformanceMap = new HashMap<ProblemInstance, LinkedHashMap<Long, Double>>();
			
		}
		/*
		 * Compute the Instances to use in the cost calculation
		 * It's everything we ran out of everything we requested.
		 */
		Set<ProblemInstance> instancesToUse = new HashSet<ProblemInstance>();
		instancesToUse.addAll(instanceSet);
		
		Set<ProblemInstance> instancesToKeep = new HashSet<ProblemInstance>(instanceSeedToPerformanceMap.keySet());
		instancesToKeep.addAll(hallucinatedValues.keySet());
		instancesToUse.retainAll(instancesToKeep);
		
		
		for(ProblemInstance pi : instancesToUse)
		{
			
			Map<Long, Double> seedToPerformanceMap = new HashMap<Long, Double>();
			if(instanceSeedToPerformanceMap.get(pi) != null) seedToPerformanceMap.putAll(instanceSeedToPerformanceMap.get(pi));
			if(hallucinatedValues.get(pi) != null) seedToPerformanceMap.putAll(hallucinatedValues.get(pi));
			
			/*
			 * Aggregate the cost over the instances
			 */
			ArrayList<Double> localCosts = new ArrayList<Double>();
			for(Map.Entry<Long, Double> ent : seedToPerformanceMap.entrySet())
			{
					localCosts.add( Math.max(minimumResponseValue, ent.getValue()) );	
			}
			instanceCosts.add( perInstanceObjectiveFunction.aggregate(localCosts,cutoffTime)); 
		}
		return aggregateInstanceObjectiveFunction.aggregate(instanceCosts,cutoffTime);
	}

	@Override
	public RunObjective getRunObjective() {
		return runObj;
	}

	@Override
	public OverallObjective getOverallObjective() {
	
		return perInstanceObjectiveFunction;
	}

	@Override
	public void incrementIteration() {
		iteration++;

	}

	@Override
	public int getIteration() {
		return iteration;
	}

	@Override
	public Set<ProblemInstance> getInstancesRan(ParamConfiguration config) {
		if (!configToPerformanceMap.containsKey(config)){
			return new HashSet<ProblemInstance>();
		}
		return new HashSet<ProblemInstance>( configToPerformanceMap.get(config).keySet() );
	}

	@Override
	public Set<ProblemInstanceSeedPair> getAlgorithmInstanceSeedPairsRan(
			ParamConfiguration config) {
		if (!configToPerformanceMap.containsKey(config)){
			return new HashSet<ProblemInstanceSeedPair>();
		}
		Set<ProblemInstanceSeedPair> pispSet = new HashSet<ProblemInstanceSeedPair>();		
		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = configToPerformanceMap.get(config);
		
		for (Entry<ProblemInstance, LinkedHashMap<Long, Double>> kv : instanceSeedToPerformanceMap.entrySet()) {
			ProblemInstance pi =  kv.getKey();
			Map<Long, Double> hConfigInst = kv.getValue();
			for (Long seed: hConfigInst.keySet()) {
				pispSet.add( new ProblemInstanceSeedPair(pi, seed) );
			}
		}
		return pispSet;
	}

	@Override
	public Set<ProblemInstanceSeedPair> getCappedAlgorithmInstanceSeedPairs(ParamConfiguration config)
	{
		if(!cappedRuns.containsKey(config))
		{
			return Collections.emptySet();
		}
		
		return Collections.unmodifiableSet(cappedRuns.get(config));
	}

	
	@Override
	public int getTotalNumRunsOfConfig(ParamConfiguration config) {
		Integer value = configToNumRunsMap.get(config);
		if( value != null)
		{
			return value;
		} else
		{
			return 0;
		}
	}

	@Override
	public double getTotalRunCost() {
		return totalRuntimeSum;
	}

	@Override
	public double[] getRunResponseValues() {
		
		double[] responseValues = new double[runHistoryList.size()];
		int i=0;
		for(RunData runData : runHistoryList)
		{
			responseValues[i] = runData.getResponseValue();
			i++;
		}
		return responseValues;
	}

	@Override
	public boolean[] getCensoredFlagForRuns() {
		boolean[] responseValues = new boolean[runHistoryList.size()];
		int i=0;
		for(RunData runData : runHistoryList)
		{
			responseValues[i] = (((runData.getRun().getRunResult().equals(RunResult.TIMEOUT) && runData.getRun().getRunConfig().hasCutoffLessThanMax())) || runData.getRun().getRunResult().equals(RunResult.KILLED));
			i++;
		}
		return responseValues;
	}

	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		return Collections.unmodifiableSet(instancesRanSet);
	}

	@Override
	public Set<ParamConfiguration> getUniqueParamConfigurations() {
		return Collections.unmodifiableSet(configToNumRunsMap.keySet());
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndex() {
		int[][] result = new int[runHistoryList.size()][2];
		
		int i=0; 
		for(RunData runData : runHistoryList)
		{
			result[i][0] = runData.getThetaIdx();
			result[i][1] = runData.getInstanceidx();
			i++;
		}
		
		return result;
	}

	@Override
	public List<ParamConfiguration> getAllParameterConfigurationsRan() {
		List<ParamConfiguration> runs = new ArrayList<ParamConfiguration>(paramConfigurationList.size());
		
		for(int i=1; i <= paramConfigurationList.size(); i++)
		{
			runs.add(paramConfigurationList.getValue(i));
		}
		return runs;
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		double[][] configs = new double[paramConfigurationList.size()][];
		for(int i=1; i <= paramConfigurationList.size(); i++)
		{
			configs[i-1] = paramConfigurationList.getValue(i).toValueArray();
		}
	
		return configs;
	}

	@Override
	/**
	 * Get a list of algorithm runs we have used
	 * 
	 * Slow O(n) method to generate a list of Algorithm Runs
	 * We could speed this up but at this point we only do this for restoring state
	 * @return list of algorithm runs we have recieved
	 */
	public List<AlgorithmRun> getAlgorithmRuns() 
	{
		
		List<AlgorithmRun> runs = new ArrayList<AlgorithmRun>(this.runHistoryList.size());
		for(RunData runData : getAlgorithmRunData() )
		{
			runs.add(runData.getRun());
		}
		return runs;
	}

	@Override
	public List<RunData> getAlgorithmRunData() {
		return Collections.unmodifiableList(runHistoryList);
	}


	@Override
	public int getThetaIdx(ParamConfiguration config) {
		Integer thetaIdx = paramConfigurationList.getKey(config);
		if(thetaIdx == null)
		{
			return -1;
		} else
		{
			return thetaIdx;
		}
		
	}
	
	@Override
	public int getOrCreateThetaIdx(ParamConfiguration config) {
		 return paramConfigurationList.getOrCreateKey(config);
		
	}
	

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(ParamConfiguration config)
	{
		 Map<ProblemInstance, LinkedHashMap<Long, Double>> runs = configToPerformanceMap.get(config);
		 
		 int total =0;
		 for(Entry<ProblemInstance, LinkedHashMap<Long, Double>> ent : runs.entrySet())
		 {
			 total+=ent.getValue().size();
		 }
		 
		 return total;
		
	}
	
	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(ParamConfiguration config)
	{
		Map<ProblemInstance, LinkedHashMap<Long,Double>> map =  configToPerformanceMap.get(config);
		if(map != null)
		{
			return Collections.unmodifiableMap(map);
		} else
		{
			return Collections.emptyMap();
		}
	}
	
	@Override
	public List<AlgorithmRun> getAlgorithmRunData(ParamConfiguration config) {
		
		List<AlgorithmRun> runs = this.configToRunMap.get(config);
		
		if(runs != null)
		{
			return Collections.unmodifiableList(runs);
		} else
		{
			return Collections.emptyList();
		}
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) 
	{

		if(seedsUsedByInstance.get(pi) == null)
		{
			seedsUsedByInstance.put(pi, new ArrayList<Long>());
		}
		return Collections.unmodifiableList(seedsUsedByInstance.get(pi));
	}



}

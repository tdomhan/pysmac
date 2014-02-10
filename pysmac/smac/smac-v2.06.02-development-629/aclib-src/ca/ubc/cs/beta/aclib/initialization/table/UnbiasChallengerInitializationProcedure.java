package ca.ubc.cs.beta.aclib.initialization.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aclib.initialization.doublingcapping.DoublingCappingInitializationProcedure;
import ca.ubc.cs.beta.aclib.initialization.doublingcapping.DoublingCappingInitializationProcedureOptions;
import ca.ubc.cs.beta.aclib.misc.associatedvalue.Pair;
import ca.ubc.cs.beta.aclib.objectives.ObjectiveHelper;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.general.TargetAlgorithmEvaluatorQueueFacade;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;
import net.jcip.annotations.NotThreadSafe;


@NotThreadSafe
public class UnbiasChallengerInitializationProcedure implements InitializationProcedure {

	private final ThreadSafeRunHistory runHistory;
	private final ParamConfiguration initialIncumbent;
	private final TargetAlgorithmEvaluator tae;
	private final UnbiasChallengerInitializationProcedureOptions opts;
	private final Logger log = LoggerFactory.getLogger(DoublingCappingInitializationProcedure.class);
	private final int maxIncumbentRuns;
	private final List<ProblemInstance> instances;
	private final InstanceSeedGenerator insc;
	private volatile ParamConfiguration incumbent;
	private final TerminationCondition termCond;
	private final double cutoffTime;
	private final SeedableRandomPool pool;
	private boolean deterministicInstanceOrdering;
	private final ParamConfigurationSpace configSpace;
	
	private final int numberOfChallengers;
	private final int numberOfRunsPerChallenger;
	
	
	private final ObjectiveHelper objHelp;
	private final double cpuTimeLimit ;

	public UnbiasChallengerInitializationProcedure(ThreadSafeRunHistory runHistory, ParamConfiguration initialIncumbent, TargetAlgorithmEvaluator tae, UnbiasChallengerInitializationProcedureOptions opts, InstanceSeedGenerator insc, List<ProblemInstance> instances,  int maxIncumbentRuns , TerminationCondition termCond, double cutoffTime, SeedableRandomPool pool, boolean deterministicInstanceOrdering, ObjectiveHelper objHelp)
	{
		this.runHistory =runHistory;
		this.initialIncumbent = initialIncumbent;
		this.tae = tae;
		this.opts = opts;
		this.instances = instances;
		this.maxIncumbentRuns = maxIncumbentRuns;
		this.insc = insc;
		this.incumbent = initialIncumbent;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.pool = pool;
		this.deterministicInstanceOrdering = deterministicInstanceOrdering;
		this.configSpace = initialIncumbent.getConfigurationSpace();
		
		this.cpuTimeLimit = opts.cpulimit;
		if(cpuTimeLimit <= 0)
		{
			throw new ParameterException("Time must be greater than zero");
		}
		//this.numberOfChallengers = opts.numberOfChallengers;
		//this.numberOfInstances = opts.numberOfInstances
		
		
		this.numberOfRunsPerChallenger = opts.numberOfRunsPerChallenger;
		this.numberOfChallengers = opts.numberOfChallengers;
		
		if(maxIncumbentRuns < numberOfRunsPerChallenger)
		{
			throw new ParameterException("Number of runs per challenger is less than the number permitted:" + maxIncumbentRuns);
		}
		
		if(!insc.allInstancesHaveSameNumberOfSeeds())
		{
			throw new ParameterException("All instances are required to have the same number of seeds available");
		}
		
		if(numberOfChallengers > this.configSpace.getUpperBoundOnSize())
		{
			throw new ParameterException("Too many challengers have been requested, configuration space size is at most " + this.configSpace.getUpperBoundOnSize() + " but we want to use " + numberOfChallengers);
		}
		
		
		if(numberOfChallengers > this.configSpace.getLowerBoundOnSize() / 10)
		{
			log.warn("Configuration space size ({}) isn't much bigger than the number of challengers we are using in initialization ({}), this isn't an error but depending on conditionality and forbidden rules, we may not be able to satisfy this requirement", configSpace.getLowerBoundOnSize(), numberOfChallengers);
		}
			
		
		if(numberOfRunsPerChallenger * numberOfChallengers <= 0)
		{
			throw new ParameterException("Challengers requested " + numberOfChallengers + " runsPerChallenger:" + numberOfRunsPerChallenger + " must both be positive");
		}
		this.objHelp = objHelp;
		
	}
	
	
	@Override
	public void run() {
		
		try 
		{
			
		
			Random rand = pool.getRandom("UNBIASED_CHALLENGER_TABLE_INITIALIZATION");
		
			List<ProblemInstanceSeedPair> selectedPisps = getProblemInstanceSeedPairs(rand);
			
			Set<ProblemInstance> selectedPis = new HashSet<ProblemInstance>();
			
			for(ProblemInstanceSeedPair pisp : selectedPisps)
			{
				selectedPis.add(pisp.getInstance());
			}
			
			Set<ParamConfiguration> thetas = getParameterConfigurations(rand, Collections.singleton(this.initialIncumbent));		
			
			List<Pair<ProblemInstanceSeedPair, ParamConfiguration>> pispConfigs = createPairs(rand, selectedPisps, thetas);
			
		
			//TargetAlgorithmEvaluatorQueueFacade<UnbiasedChallengerInitializationProcedureContext> tque =  new TargetAlgorithmEvaluatorQueueFacade<UnbiasedChallengerInitializationProcedureContext>(tae, true); 
			
			final List<AlgorithmRun> incumbentRuns = scheduleInitialIncumbent(selectedPisps);
			
			initializeRuns(pispConfigs);
			
			
			log.debug("Waiting for all outstanding evaluations to complete");
			tae.waitForOutstandingEvaluations();
			log.info("All outstanding runs completed, inspecting best configuration");
			
			
			
			if(incumbentRuns.size() == 0)
			{
				throw new IllegalStateException("Expected Default Configuration to have finished running at this point");
			}
			
			try {
				runHistory.append(incumbentRuns);
			} catch (DuplicateRunException e) {
				throw new IllegalStateException(e);
			}
			
			
			
			ParamConfiguration bestConfiguration = selectMinimumConfiguration(selectedPis, thetas);
			
			double minCost = runHistory.getEmpiricalCost(bestConfiguration, selectedPis , this.cutoffTime);
			double incCost = runHistory.getEmpiricalCost(initialIncumbent, selectedPis, this.cutoffTime);
			
			if(incCost > minCost)
			{
				log.info("Challenger {} ({}) looks better than initial incumbent {} ({}) scheduling censored runs ( {} vs. {} )", runHistory.getThetaIdx(bestConfiguration),bestConfiguration, runHistory.getThetaIdx(initialIncumbent) , initialIncumbent,  minCost, incCost);
				
				List<AlgorithmRun> runs = runHistory.getAlgorithmRunData(bestConfiguration);
				
				
				
				Set<ProblemInstanceSeedPair> pispsToRun = new HashSet<ProblemInstanceSeedPair>();
				
						
				for(AlgorithmRun run : runs)
				{
					if(!run.getRunResult().isDecided() && run.getRunConfig().hasCutoffLessThanMax())
					{
						pispsToRun.add(run.getRunConfig().getProblemInstanceSeedPair());
					}
				}
				
				
				List<RunConfig> rcs = new ArrayList<RunConfig>();
				for(ProblemInstanceSeedPair pisp : pispsToRun)
				{
					rcs.add(new RunConfig(pisp, this.cutoffTime, bestConfiguration, false));
				}
				
				log.info("Solved runs {} ", runHistory.getAlgorithmRunData(bestConfiguration));
				log.info("Unsolved {}",rcs);
				log.info("Scheduling {} incomplete runs for {} ({}) ", rcs.size() , runHistory.getThetaIdx(bestConfiguration), bestConfiguration );
				
				List<AlgorithmRun> completedRuns = tae.evaluateRun(rcs);
				try {
					runHistory.append(completedRuns);
				} catch (DuplicateRunException e) {
					throw new IllegalStateException(e);
				}
				
				minCost = runHistory.getEmpiricalCost(bestConfiguration, selectedPis , this.cutoffTime);
			}
			
			if(incCost > minCost)
			{
				log.info("Best challengers performance on set is {} versus initial incumbent {}, setting new incumbent", minCost, incCost);
				this.incumbent = bestConfiguration;
				
			} else
			{
				
				log.info("Best challengers performance on set is {} versus initial incumbent {}, leaving incumbent alone", minCost, incCost);
				this.incumbent = initialIncumbent;
			}
			
		
			log.info("Initialization procedure completed ({} total runs, total cpu time used {}), selected incumbent is {} ({})", this.runHistory.getAlgorithmRunData().size(), this.runHistory.getTotalRunCost(), this.runHistory.getThetaIdx(this.incumbent), this.incumbent.getFriendlyIDHex());
			
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			this.incumbent = initialIncumbent;
			return;
			
		}
		
		
		
		
		System.exit(0);
		
		
		
	}


	/**
	 * @param selectedPis
	 * @param thetas
	 * @return
	 */
	private ParamConfiguration selectMinimumConfiguration(
			Set<ProblemInstance> selectedPis, Set<ParamConfiguration> thetas) {
		double minCost = Double.MAX_VALUE;
		ParamConfiguration bestConfiguration = null;
		
		for(ParamConfiguration config : thetas)
		{
			double cost = runHistory.getEmpiricalCost(config, selectedPis , this.cutoffTime);
			
			if(bestConfiguration == null)
			{
				bestConfiguration = config;
			}
			
			if(minCost > cost)
			{
				bestConfiguration = config;
				minCost = cost;
			}
			
		}
		return bestConfiguration;
	}


	/**
	 * Preforms the bulk of the initialization of runs
	 * @param pispConfigs 	The pairs that will be initialized
	 * @throws InterruptedException
	 */
	private void initializeRuns(List<Pair<ProblemInstanceSeedPair, ParamConfiguration>> pispConfigs) throws InterruptedException 
	{

		final BlockingQueue<Pair<ProblemInstanceSeedPair, ParamConfiguration>> unSolvedPispThetasQueue = new LinkedBlockingQueue<Pair<ProblemInstanceSeedPair, ParamConfiguration>>();
		
		unSolvedPispThetasQueue.addAll(pispConfigs);
		
		final Set<Pair<ProblemInstanceSeedPair, ParamConfiguration>> solvedPisps = Collections.newSetFromMap(new ConcurrentHashMap<Pair<ProblemInstanceSeedPair, ParamConfiguration>, Boolean>());
		
		final AtomicBoolean killNow = new AtomicBoolean(false);
		
		TargetAlgorithmEvaluatorRunObserver killAtTimeoutObserver = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				if(killNow.get())
				{
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
				
			}
			
		};
		
		final int allPairsSize = pispConfigs.size();
		
		final Semaphore runCompleted = new Semaphore(unSolvedPispThetasQueue.size());
		
		
		/**
		 * Algorithm is as follows:
		 * 
		 *  while(true)
		 *  {
		 *  	for(each pispConfig )
		 *  	
		 *  
		 *  
		 *  
		 *  
		 *  }
		 * 
		 */
		
		
		
		
		
		Map<Pair<ProblemInstanceSeedPair, ParamConfiguration>, Double> nextCutoffTime = new ConcurrentHashMap<Pair<ProblemInstanceSeedPair, ParamConfiguration>, Double>();

		
		
		for(Pair<ProblemInstanceSeedPair, ParamConfiguration> p : pispConfigs)
		{
			nextCutoffTime.put(p, Double.valueOf(-1));
		}
	
outOfInitialization:
		while(true)
		{
			
			//Submit everything until you can't submit any more.
			for(int i=0; i < pispConfigs.size(); i++)
			{
				
				while(!runCompleted.tryAcquire(1, TimeUnit.SECONDS));
				
				
				final Pair<ProblemInstanceSeedPair, ParamConfiguration> pair = unSolvedPispThetasQueue.poll();
		
				if(pair == null)
				{					
					//Everything is solved so we are done
					if(solvedPisps.size() == allPairsSize)
					{
						log.debug("All runs are considered done");
						break outOfInitialization;
					}
					
					//Something finished, but we don't have anything unfinished, so we will continue;
					i--;
					continue;
				}

				TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback() {
					
					@Override
					public void onSuccess(List<AlgorithmRun> runs) {
						
						try {
							runHistory.append(runs);
						} catch (DuplicateRunException e) {
							throw new IllegalStateException(e);
						}

						//== Either it was decided or it has a run at kappaMax
						if(runs.get(0).getRunResult().isDecided() || !runs.get(0).getRunConfig().hasCutoffLessThanMax())
						{
							if(runs.get(0).getRunResult().isDecided())
							{
								log.info("Run completed successfully: {} " ,runs.get(0));
							}
							solvedPisps.add(pair);
						} else
						{
							unSolvedPispThetasQueue.add(pair);
						}
						
						runCompleted.release();
					}
					
					@Override
					public void onFailure(RuntimeException e) {
						log.error("Error occurred during initialization", e);
						
					}
				};
				
				double kappa = getNextKappa(pair,allPairsSize,solvedPisps.size(),nextCutoffTime.get(pair));
				
				if(runHistory.getTotalRunCost() > this.cpuTimeLimit)
				{
					killNow.set(true);
					
					log.info("Initialization procedure has used {}, time limit: {}, halting procedure...", runHistory.getTotalRunCost(), this.cpuTimeLimit);
					break outOfInitialization;
				}
				
				if(kappa < 0)
				{
					throw new IllegalStateException("Still continuing with run, but the kappa time is zero.");
				}
				
				nextCutoffTime.put(pair, kappa);
				tae.evaluateRunsAsync(Collections.singletonList(new RunConfig(pair.getFirst(), kappa, pair.getSecond(), kappa < cutoffTime)), taeCallback, killAtTimeoutObserver);
			
				
			}
		
		}
	}


	/**
	 * 
	 * @param pair
	 * @param allPairsSize
	 * @param solvedPisSize
	 * @param lastKappa
	 * @return
	 */
	public double getNextKappa(Pair<ProblemInstanceSeedPair, ParamConfiguration> pair, int allPairsSize, int solvedPisSize, double lastKappa)
	{
		
		double kappa;
		
		double kappaFromShareRemaining = (this.cpuTimeLimit - this.runHistory.getTotalRunCost())/(allPairsSize-solvedPisSize);
		double kappaNextStep = 2*lastKappa;
		double kappaMax = this.cutoffTime;
		kappa = Math.min(Math.max(kappaNextStep, kappaFromShareRemaining), kappaMax);
	
		return Math.max(kappa,1);
		
		
	}
	
	@Override
	public ParamConfiguration getIncumbent() {
		return incumbent;
	}	
	
	/**
	 * Schedules the runs for the initial incumbent
	 * @param selectedPisps
	 * @return
	 */
	private List<AlgorithmRun> scheduleInitialIncumbent(List<ProblemInstanceSeedPair> selectedPisps) {
		
		
		
		final List<AlgorithmRun> incumbentRuns = Collections.synchronizedList(new ArrayList<AlgorithmRun>(selectedPisps.size()));
		
		List<RunConfig> rcs = new ArrayList<RunConfig>(selectedPisps.size());
		
		for(ProblemInstanceSeedPair pisp : selectedPisps)
		{
			rcs.add(new RunConfig(pisp, cutoffTime, initialIncumbent));
		}
		
		runHistory.getOrCreateThetaIdx(initialIncumbent);
		log.info("Scheduling {} runs for initial configuration", rcs.size());
		tae.evaluateRunsAsync(rcs, new TargetAlgorithmEvaluatorCallback() {

			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
				log.info("Default configuration runs are done");
				incumbentRuns.addAll(runs);
			}

			@Override
			public void onFailure(RuntimeException e) {
				log.error("Error occurred during initialization", e);
				
			}
			
		});
		return incumbentRuns;
	}
	

	/**
	 * Creates a shuffled list of pair objects
	 * 
	 * @param rand
	 * @param selectedPisps
	 * @param thetas
	 * @return
	 */
	private List<Pair<ProblemInstanceSeedPair, ParamConfiguration>> createPairs(Random rand, List<ProblemInstanceSeedPair> selectedPisps,Set<ParamConfiguration> thetas) {
		List<Pair<ProblemInstanceSeedPair, ParamConfiguration>> pispConfigs = new ArrayList<Pair<ProblemInstanceSeedPair, ParamConfiguration>>(thetas.size() * selectedPisps.size() );
		for(ParamConfiguration config : thetas)
		{
			for(ProblemInstanceSeedPair pisp : selectedPisps)
			{
				pispConfigs.add(new Pair<ProblemInstanceSeedPair, ParamConfiguration>(pisp, config));
			}
		}
		
		Collections.shuffle(pispConfigs,rand);
		return pispConfigs;
	}


	/**
	 * Returns a set of distinct parameter configurations
	 * 
	 * @param rand
	 * @return
	 */
	private Set<ParamConfiguration> getParameterConfigurations(Random rand, Set<ParamConfiguration> excluded) {
		log.info("Generating {} configurations for use in initialization", this.numberOfChallengers);
		Set<ParamConfiguration> thetas = new HashSet<ParamConfiguration>(this.numberOfChallengers);
		
		thetas.addAll(excluded);
		//=== Create Thetas for table
		while((thetas.size() -  excluded.size()) < numberOfChallengers )
		{
			boolean created = false;
			for(int i=0; i < 1000; i++)
			{
				ParamConfiguration config = configSpace.getRandomConfiguration(rand);

				if(config.equals(configSpace.getDefaultConfiguration()))
				{
					i--;
					continue;
				}
					
				boolean newConfiguration = thetas.add(config);
				
				if(newConfiguration)
				{
					created = true;
					break;
				}
			}
			
			if(!created)
			{
				throw new IllegalStateException("After 1000 attempts we were unable to generate another unique configuration. We already have " + thetas.size() + " this can happen if you request too many configurations in initialization, or if the space is incredibly conditional / has many forbidden configurations." );
			}
			
		}
		thetas.removeAll(excluded);
		return thetas;
	}


	/**
	 * Returns a set of problem instance seed pairs. The most frequently occurring instance should only occur at most once more than the least frequently occuring.
	 * @param rand
	 * @return List of problem instance seed pairs
	 */
	private List<ProblemInstanceSeedPair> getProblemInstanceSeedPairs(Random rand) {
		List<ProblemInstanceSeedPair> selectedPisps = new ArrayList<ProblemInstanceSeedPair>(); 
		
		log.info("Generating {} Problem Instance Seed Pairs for use in initialization", numberOfRunsPerChallenger);
		

		List<ProblemInstance> shuffledPis = new ArrayList<ProblemInstance>(instances);
		
		Collections.shuffle(shuffledPis,rand);
		
		//=== Create PISPS for table
		while(selectedPisps.size() < numberOfRunsPerChallenger)
		{
			for(ProblemInstance pi : shuffledPis)
			{
				
				if(!insc.hasNextSeed(pi))
				{
					throw new IllegalStateException("Should not have been able to generate this many requests for configurations");
				}
				
				long seed = insc.getNextSeed(pi);
				ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, seed);
				selectedPisps.add(pisp);
				
				if(selectedPisps.size() >= numberOfRunsPerChallenger)
				{
					break;
				}
				
			}
		}
		return selectedPisps;
	}
	

}

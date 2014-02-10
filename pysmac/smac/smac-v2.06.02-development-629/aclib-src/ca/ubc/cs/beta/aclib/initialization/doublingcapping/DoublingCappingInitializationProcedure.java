package ca.ubc.cs.beta.aclib.initialization.doublingcapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.exceptions.OutOfTimeException;
import ca.ubc.cs.beta.aclib.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aclib.misc.MapList;
import ca.ubc.cs.beta.aclib.objectives.ObjectiveHelper;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.seedgenerator.SetInstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

@NotThreadSafe
public class DoublingCappingInitializationProcedure implements InitializationProcedure {

	private final ThreadSafeRunHistory runHistory;
	private final ParamConfiguration initialIncumbent;
	private final TargetAlgorithmEvaluator tae;
	private final DoublingCappingInitializationProcedureOptions opts;
	private final Logger log = LoggerFactory.getLogger(DoublingCappingInitializationProcedure.class);
	private final int maxIncumbentRuns;
	private final List<ProblemInstance> instances;
	private final InstanceSeedGenerator insc;
	private ParamConfiguration incumbent;
	private final TerminationCondition termCond;
	private final double cutoffTime;
	private final SeedableRandomPool pool;
	private boolean deterministicInstanceOrdering;
	private final ParamConfigurationSpace configSpace;
	
	private final int numberOfChallengers;
	private final int numberOfRunsPerChallenger;
	
	private final ObjectiveHelper objHelp;

	public DoublingCappingInitializationProcedure(ThreadSafeRunHistory runHistory, ParamConfiguration initialIncumbent, TargetAlgorithmEvaluator tae, DoublingCappingInitializationProcedureOptions opts, InstanceSeedGenerator insc, List<ProblemInstance> instances,  int maxIncumbentRuns , TerminationCondition termCond, double cutoffTime, SeedableRandomPool pool, boolean deterministicInstanceOrdering, ObjectiveHelper objHelp)
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
		
		this.numberOfChallengers = opts.numberOfChallengers;
		this.numberOfRunsPerChallenger = opts.numberOfRunsPerChallenger;
		this.objHelp = objHelp;
		
	}
	
	@Override
	public void run()
	{
		
		log.warn("Doubling Capping initialization procedure is EXPERIMENTAL currently. It may not work in all scenarios, such as those with small configurations and/or instance distributions. Termination conditions will be updated but not actually checked until after the procedure is completed, and state restoration will not properly restore the state (some runs will be lost). Finally as it has a lot of edge cases bugs are likely, the bugs should only manifest themselves as a crash");
		
		
		if(numberOfChallengers == 1)
		{
			throw new ParameterException("Number of Challengers must be greater than 1, use CLASSIC initialization ");
		}
		log.error("TAE Notify and Events need to be handled");
		log.info("Using Doubling Capping Initialization");
		ParamConfiguration incumbent = this.initialIncumbent;
		log.info("Configuration Set as initial Incumbent: {}", incumbent);

		double startKappa=cutoffTime;
		//Start kappa at the lowest value that is greater than 1, and perfectly divisible from kappaMax.
		
		int divisions = 1;
		while(startKappa/2 > 1)
		{
			startKappa/=2;
			divisions++;
		}
	
		
		Set<ParamConfiguration> randomConfigurations = new HashSet<ParamConfiguration>();
		
		int totalFirstRoundChallengers = numberOfChallengers * numberOfRunsPerChallenger * divisions;
		if(totalFirstRoundChallengers > configSpace.getUpperBoundOnSize())
		{
			throw new IllegalStateException("Doubling Capping initialization won't work with this configuration space as it's too small, use classic");
		}
		
		
		
		
		
		//Get enough random configurations for the first round
		Random configRandom = pool.getRandom("DOUBLING_INITIALIZATION_CONFIGS");
		while(randomConfigurations.size() < totalFirstRoundChallengers)
		{
			randomConfigurations.add(configSpace.getRandomConfiguration(configRandom));
		}
		
		
		List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>(totalFirstRoundChallengers);
		
		//Generate enough problem instance seed pairs for the first round
		//If we make 10000 attempts without getting a configuration we will abort
		Random pispRandom = pool.getRandom("DOUBLING_INITIALIZATION_PISPS");
		for(int i=0, attempts=0; i < totalFirstRoundChallengers; i++, attempts++)
		{
			if(insc instanceof SetInstanceSeedGenerator)
			{
				//We will always use the same seed
				insc.reinit();
			}
			
			ProblemInstance pi =instances.get(pispRandom.nextInt(instances.size()));
			
			if(!insc.hasNextSeed(pi))
			{
				i--;
				if(attempts > 10000)
				{
					throw new IllegalStateException("Could not generate anymore problem instance seed pairs, probably the number of instances * number of seeds is too small compared to the number of challengers and runs per challenge to use in initialization");
				}
				continue; 
			} else
			{
				attempts=0;
			}
			
		
			ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi,insc.getNextSeed(pi));
			
			pisps.add(pisp);
			
		}
		
		
		log.debug("Doubling capping has generated {} distinct configurations and {} problem instance seed pairs", randomConfigurations.size(), pisps.size());
		/**
		 * Construct a giant queue of runConfigs to do essentially everything we will do in the first round
		 * (That is over all configurations, all problem instance seed pairs, all cutofftimes)
		 * 
		 */
		
		
		LinkedBlockingQueue<ParamConfiguration> configsQueue = new LinkedBlockingQueue<ParamConfiguration>();
		configsQueue.addAll(randomConfigurations);
		
		LinkedBlockingQueue<ProblemInstanceSeedPair> pispsQueue = new LinkedBlockingQueue<ProblemInstanceSeedPair>();
		pispsQueue.addAll(pisps);
		
		LinkedBlockingQueue<RunConfig> runsToDo = new LinkedBlockingQueue<RunConfig>();
		
		for(double kappa = startKappa ; kappa <= cutoffTime; kappa*=2)
		{
			for(int i=0; i < numberOfChallengers * numberOfRunsPerChallenger; i++)
			{
				ParamConfiguration config;
				if(i == 0)
				{
					config = initialIncumbent;
				} else
				{
					config = configsQueue.poll();
				}
				
				RunConfig rc = new RunConfig(pispsQueue.poll(), kappa, config, kappa < cutoffTime);
				runsToDo.add(rc);
			}
		}
	
		log.debug("Doubling capping has generated {} runs to do", runsToDo);

		
		
		
		
		
		
		
		
	
		
		
		MapList<RunResult, AlgorithmRun> runs = new MapList<RunResult,AlgorithmRun>(new EnumMap<RunResult,List<AlgorithmRun>>(RunResult.class));
		
		
		phaseOneRuns(runsToDo, runs);
		
		
		/***
		 * Phase two schedule every configuration on every problem instance seed pair.
		 * We will generate PISPS with SAT/UNSAT response first, them timeout, then 
		 * 
		 * 
		 */
			
		Set<AlgorithmRun> phaseTwoRuns = new HashSet<AlgorithmRun>();
		
		
		//We will add SAT and UNSAT responses for sure
		phaseTwoRuns.addAll(runs.getList(RunResult.SAT));
		phaseTwoRuns.addAll(runs.getList(RunResult.UNSAT));
		
		
		if(phaseTwoRuns.size() <  numberOfChallengers )
		{
			log.info("Insufficient runs with SAT and UNSAT were found {} but needed {}, using some TIMEOUT runs for Phase 2 of initialization", phaseTwoRuns.size(), numberOfChallengers);
			
			int i=0; 
			List<AlgorithmRun> timeouts = runs.getList(RunResult.TIMEOUT);
			
			while((phaseTwoRuns.size() < numberOfChallengers) && i < timeouts.size())
			{
				phaseTwoRuns.add(timeouts.get(i));
				i++;
			}
		}
		
		//Generate all the random instances required
		
		if(phaseTwoRuns.size() < numberOfChallengers)
		{
			log.info("Phase one did not have enough completed runs ({}) to satisfy request of challengers: {}", phaseTwoRuns.size(), numberOfChallengers); 
		} else
		{
			log.info("Beginning Phase 2 of initialization with {} completed runs", phaseTwoRuns.size());
		}
		
		
		Set<RunConfig> existingRunConfigs = new HashSet<RunConfig>();
		Set<ParamConfiguration> configs = new HashSet<ParamConfiguration>();
		Set<ProblemInstanceSeedPair> phaseTwoPisps = new HashSet<ProblemInstanceSeedPair>();
		MapList<ParamConfiguration, AlgorithmRun> phaseTwoResults = new MapList<ParamConfiguration, AlgorithmRun>(new HashMap<ParamConfiguration, List<AlgorithmRun>>());
		MapList<ParamConfiguration, ProblemInstanceSeedPair> phaseTwoPispResults = MapList.getHashMapList();
		
		final Map<ParamConfiguration, AlgorithmRun> previouslyExistingRun = new ConcurrentHashMap<ParamConfiguration, AlgorithmRun>();
		
		
 		for(AlgorithmRun run : phaseTwoRuns)
		{
			existingRunConfigs.add(run.getRunConfig());
			configs.add(run.getRunConfig().getParamConfiguration());
			phaseTwoPisps.add(run.getRunConfig().getProblemInstanceSeedPair());
			phaseTwoResults.addToList(run.getRunConfig().getParamConfiguration(), run);
			
			phaseTwoPispResults.addToList(run.getRunConfig().getParamConfiguration(), run.getRunConfig().getProblemInstanceSeedPair());
			
			if(phaseTwoResults.get(run.getRunConfig().getParamConfiguration()).size() > 1)
			{
				log.warn("[BUG Detected]: Expected that only run one would be completed for a given configuration, but got {}", phaseTwoResults.get(run.getRunConfig().getParamConfiguration()));
			}
		}
		
 		
 		List<ParamConfiguration> configToIterate = new ArrayList<ParamConfiguration>(configs);
 		
 		Random diShuffle = pool.getRandom("DOUBLING_INITIALIZATION_SHUFFLE");
 		
 		Collections.shuffle(configToIterate, diShuffle);
 		if(!configs.contains(initialIncumbent))
 		{
 			log.debug("Initial Incumbent did not pass first round, adding to set");
 			
 			configToIterate.add(configToIterate.get(0));
 			configToIterate.set(0, initialIncumbent);
 		}
 		
 		List<ProblemInstanceSeedPair> pispsToIterate = new ArrayList<ProblemInstanceSeedPair>(phaseTwoPisps);
 		Collections.shuffle(pispsToIterate, diShuffle);
 		
 		final AtomicDouble bestPerformance = new AtomicDouble(Double.MAX_VALUE);
 		
 		
 		
 		TargetAlgorithmEvaluatorRunObserver phaseTwoObs = new TargetAlgorithmEvaluatorRunObserver()
		{
			@Override
			public void currentStatus(	List<? extends KillableAlgorithmRun> runs)
			{
				List<AlgorithmRun> objRuns = new ArrayList<AlgorithmRun>(runs);
				
				objRuns.add(previouslyExistingRun.get(runs.get(0).getRunConfig().getParamConfiguration()));
				double myPerformance = objHelp.computeObjective(runs);
				if(myPerformance > bestPerformance.get())
				{
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
					
					return;
				} 
			}
		};
		
		
		BasicTargetAlgorithmEvaluatorQueue phaseTwoTaeQueue = new BasicTargetAlgorithmEvaluatorQueue(tae, true);
		
		//We generally will submit a run we have already completed before
		//the amount of code to handle the case where we haven't completed before is annoying and 
		//complicates this so essentially we will rely on caching.
 		for(int i=0; i < Math.min(numberOfChallengers, configToIterate.size()); i++)
 		{
 			ParamConfiguration config = configToIterate.get(i);
 			List<RunConfig> runsForConfig = new ArrayList<RunConfig>();
 			
 			for(int j=0; j < Math.min(this.numberOfRunsPerChallenger, phaseTwoPisps.size()); j++)
 			{
 				ProblemInstanceSeedPair pisp = pispsToIterate.get(j);
 				runsForConfig.add(new RunConfig(pisp, this.cutoffTime, config));
 			}
 			log.debug("Scheduling {} runs for config {}", runsForConfig.size(), config);
			phaseTwoTaeQueue.evaluateRunAsync(runsForConfig, phaseTwoObs);
 		}
 		
 		
 		ParamConfiguration newIncumbent = null;
 		
 		while(phaseTwoTaeQueue.getNumberOfOutstandingAndQueuedRuns() > 0)
 		{
 			try {
				List<AlgorithmRun> currentResults = phaseTwoTaeQueue.take().getAlgorithmRuns();
				double myPerformance = objHelp.computeObjective(currentResults);
				
				//There are no data races here because only one thread will ever update the value.
				if(bestPerformance.get() > myPerformance)
				{
					double previousBest = bestPerformance.get();
					
					bestPerformance.set(myPerformance);
					newIncumbent = currentResults.get(0).getRunConfig().getParamConfiguration();
					log.debug("New Incumbent set to {} with performance {} previous best was {} ", newIncumbent, myPerformance, previousBest);
				}
				try {
					for(AlgorithmRun run : currentResults)
					{
						this.insc.take(run.getRunConfig().getProblemInstanceSeedPair().getInstance(), run.getRunConfig().getProblemInstanceSeedPair().getSeed());
					}
					this.runHistory.append(currentResults);
				} catch (DuplicateRunException e) {
					throw new IllegalStateException(e);
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted Exception occurred during start up, cannot continue, every invariant I am designed to hold true can not be assured.");
			}
 			
 		}
 		
 		
 		
 		log.info("Initialization Procedure Completed. Selected incumbent {} ({}) incumbent has performance: {} ", this.runHistory.getThetaIdx(incumbent), incumbent, bestPerformance.get());
 		this.incumbent = newIncumbent;
 		
	}

	/**
	 * Complete Phase One of the Initialization Procedure 
	 * 
	 * Loosely this corresponds to running on a bunch of different problem instance seed pairs, with increasing cap times until some number of runs is complete
	 * @param runsToDo A list of pregenerated run configurations that are in order of increasing cap times
	 * @param runs
	 */
	private void phaseOneRuns(LinkedBlockingQueue<RunConfig> runsToDo,
			MapList<RunResult, AlgorithmRun> runs) {
		
		/**
		 * Essentially this block of code is doing the following:
		 * 
		 * While completed runs < numberOfChallengers.
		 * 			
		 * 		Take the next runToDo (if it is the incumbent and we have a solved run, skip it).
		 * 		Schedule the run
		 * 		While there are runs that are done:
		 * 			If the run is SAT or UNSAT increase the number of completed runs (and keep track if it was the incumbent). 
		 * 			If the run is anything else and has cutoff of Kappa Max then increase the number of completed runs.
		 * 		
		 * At the end it is expected that we have numerous PISPS to make a selection with
		 */
		
		
		
		int completedRuns = 0;
		final AtomicBoolean allRunsCompleted = new AtomicBoolean(false);
		AtomicBoolean incumbentSolved = new AtomicBoolean(false);
		TargetAlgorithmEvaluatorRunObserver obs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				if(allRunsCompleted.get())
				{
					log.debug("Phase One completed killing in progress runs {}", runs);
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
				
			}
			
			
		};
		
		log.info("Beginning Phase One Runs");
		double lastKappa = 0;
		BasicTargetAlgorithmEvaluatorQueue taeQueue = new BasicTargetAlgorithmEvaluatorQueue(tae, true);
		
topOfLoop:
		while(completedRuns < numberOfChallengers)
		{
		
		
			try 
			{
				RunConfig rc = runsToDo.poll();
				
				BasicTargetAlgorithmEvaluatorQueueResultContext context;
				if(rc != null)
				{
					while((incumbentSolved.get() == true) && rc.getParamConfiguration().equals(initialIncumbent))
					{
						rc = runsToDo.poll();
						if(rc == null)
						{
							continue topOfLoop;
						}
					}
						
					if(lastKappa != rc.getCutoffTime())
					{
						lastKappa = rc.getCutoffTime();
						log.info("Beginning Phase One Runs with Cutoff time {} (s)", lastKappa);
					}
					taeQueue.evaluateRunAsync(Collections.singletonList(rc), obs);
					
					//More runs can be enqueued right away
					context = taeQueue.poll();
				} else
				{
					//Wait for something to finish
					context = taeQueue.take();
				}
				
				 
				
				while(context != null)
				{
					AlgorithmRun run = context.getAlgorithmRuns().get(0);
					log.debug("Run Returned: {}", run);
					
					switch(run.getRunResult())
					{
					
						case SAT:
						case UNSAT:
							
							if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
							{
								if(incumbentSolved.get() == false)
								{
									incumbentSolved.set(true);
									completedRuns++;
									log.debug("Run completed need {} more", numberOfChallengers - completedRuns);
									runs.addToList(run.getRunResult(), run);
								} else
								{
									break;
								}
							} else
							{
								completedRuns++;
								log.debug("Run completed need {} more", numberOfChallengers - completedRuns);
								runs.addToList(run.getRunResult(), run);
							}
							break;
						case TIMEOUT:
							if(!run.getRunConfig().hasCutoffLessThanMax())
							{
								if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
								{
									if(incumbentSolved.get() == false)
									{
										incumbentSolved.set(true);
										completedRuns++;
										log.debug("Run completed need {} more", numberOfChallengers - completedRuns);
										runs.addToList(run.getRunResult(), run);
									} else
									{
										break;
									}
									
								} else
								{
									completedRuns++;
									log.debug("Run completed need {} more", numberOfChallengers - completedRuns);
									runs.addToList(run.getRunResult(), run);
								}
							}
							break;
						case KILLED:
							  log.debug("Killed run detected in First round: {}", run);
							break;
							
							
							
						case CRASHED:
							if(!run.getRunConfig().hasCutoffLessThanMax())
							{
								if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
								{
									incumbentSolved.set(true);
								}
								completedRuns++;
								log.debug("Run completed need {} more", numberOfChallengers - completedRuns);
								runs.addToList(run.getRunResult(), run);
							}
							
							break;
						default:
							throw new IllegalStateException("Got unexpected run result back " + context.getAlgorithmRuns().get(0).getRunResult());
					}
			
					context = taeQueue.poll();
				}
			} catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Thread interrupted prematurely");
				
			}
		}
		log.debug("Notifying existing Phase One runs to terminate");
		allRunsCompleted.set(true);
		log.info("Phase One Runs Complete");
	}

	@Override
	public ParamConfiguration getIncumbent() {
		return incumbent;
	}
	
	
	
	


}

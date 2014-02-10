package ca.ubc.cs.beta.aclib.example.statemerge;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.input.ReaderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.exceptions.DeveloperMadeABooBooException;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.MapList;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.string.SplitQuotedString;
import ca.ubc.cs.beta.aclib.misc.version.VersionTracker;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.NewRunHistory;
import ca.ubc.cs.beta.aclib.runhistory.ReindexSeedRunHistoryDecorator;
import ca.ubc.cs.beta.aclib.runhistory.RunData;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistoryWrapper;
import ca.ubc.cs.beta.aclib.state.StateFactoryOptions;
import ca.ubc.cs.beta.aclib.state.StateSerializer;
import ca.ubc.cs.beta.aclib.state.legacy.LegacyStateFactory;
import ca.ubc.cs.beta.models.fastrf.RandomForest;
import ec.util.MersenneTwister;

public class StateMergeExecutor {

	private static  Logger log = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		StateMergeOptions smo = new StateMergeOptions();

		try 
		{
			JCommander jcom;
			try {
			 jcom = JCommanderHelper.parseCheckingForHelpAndVersion(args, smo);
			} finally
			{
				smo.logOpts.initializeLogging();
				log = LoggerFactory.getLogger(StateMergeExecutor.class);
				VersionTracker.logVersions();
			}
			
			
			for(String file : jcom.getParameterFilesToRead())
			{
				log.info("Reading options from default file {} " , file);
			}
			
			
			
			log.info("Determining Scenario Options");
			List<ProblemInstance> pis = smo.scenOpts.getTrainingAndTestProblemInstances(smo.experimentDir, 0, 0, true, false, false, false).getTrainingInstances().getInstances();;
			AlgorithmExecutionConfig execConfig = smo.scenOpts.getAlgorithmExecutionConfigSkipExecDirCheck(smo.experimentDir);
			MapList<Integer, AlgorithmRun> runsPerIteration = new MapList<Integer, AlgorithmRun>(new LinkedHashMap<Integer, List<AlgorithmRun>>());
			
			if(execConfig.isDeterministicAlgorithm())
			{
				smo.replaceSeeds = false;
			} else
			{
				smo.replaceSeeds = true;
			}
			log.info("Scanning directories");
			Set<String> directoriesWithState = getAllRestoreDirectories(smo.directories);
			
			
			
			
			for(String dir: directoriesWithState)
			{
				extractRunsFromDirectory(smo, pis, execConfig, runsPerIteration, dir);
			}
			
			
			Map<String, ProblemInstance> fixedPi = new LinkedHashMap<String, ProblemInstance>();
			
			
			
			
			MapList<Integer, AlgorithmRun> repairedRuns = repairProblemInstances(runsPerIteration, fixedPi);
			
			
			Random r = new MersenneTwister(smo.seed);
			
			log.info("Processing Runs");
			RunHistory rh = new NewRunHistory(smo.scenOpts.intraInstanceObj, smo.scenOpts.interInstanceObj, smo.scenOpts.runObj);
			if(smo.replaceSeeds)
			{
				rh = new ReindexSeedRunHistoryDecorator(rh,r );
			}
			ThreadSafeRunHistory rhToFilter = new ThreadSafeRunHistoryWrapper(rh);
			
			for(Entry<Integer, List<AlgorithmRun>> itToRun :repairedRuns.entrySet())
			{
				
				for(AlgorithmRun run : itToRun.getValue())
				{
					try {
						
						rhToFilter.append(run);
					} catch (DuplicateRunException e) {
					
						e.printStackTrace();
					}
				}
				rhToFilter.incrementIteration();
			}
			
			
			int rdi=0;
			for(RunData rd : rhToFilter.getAlgorithmRunData())
			{
				rdi++;
				log.debug("Restored Data Iteration {} => {} ", rd.getIteration(), rd.getRun());
			}
			log.info("Restored Runs Count {} ", rdi);
			
			ThreadSafeRunHistory rhToSaveToDisk;
			
			ParamConfiguration newIncumbent = null;
			if(smo.repairMaxRunsForIncumbentInvariant)
			{
				
				
				List<ParamConfiguration> configs = rhToFilter.getAllParameterConfigurationsRan();
				
				
				
				Set<ProblemInstanceSeedPair> allPisps = new HashSet<ProblemInstanceSeedPair>();
				
				ParamConfiguration maxConfig = null;
				
				Set<ParamConfiguration> maxConfigs = new HashSet<ParamConfiguration>();
				int maxSetSize = 0;
				for(ParamConfiguration config : configs)
				{
					log.info("Number of runs for configuration {} is {}", config, rhToFilter.getAlgorithmInstanceSeedPairsRan(config).size());
					
					allPisps.addAll(rhToFilter.getAlgorithmInstanceSeedPairsRan(config));
					if(maxSetSize < rhToFilter.getAlgorithmInstanceSeedPairsRan(config).size())
					{
						maxConfigs.clear();
						maxConfigs.add(config);
						maxSetSize = rhToFilter.getAlgorithmInstanceSeedPairsRan(config).size();
						
					} else if(maxSetSize == rhToFilter.getAlgorithmInstanceSeedPairsRan(config).size())
					{
						maxConfigs.add(config);
						//maxSetSize = rhToFilter.getAlgorithmInstanceSeedPairsRan(config).size();
					}
					
				}
				
				log.info("Number of possible incumbents are {}", maxConfigs.size());
				
				StateMergeModelBuilder smmb = new StateMergeModelBuilder();
				
				List<ProblemInstance> instances = new ArrayList<ProblemInstance>();
				instances.addAll(fixedPi.values());
				
				 SeedableRandomPool srp = new SeedableRandomPool(1);
				log.info("Building model");
				
				boolean adaptiveCapping = true;
				if(smo.rfo.logModel == null)
				{
					if(smo.scenOpts.runObj.equals(RunObjective.RUNTIME))
					{
						smo.rfo.logModel = true;
					}  else
					{
						smo.rfo.logModel = false;
						adaptiveCapping = false;
					}
				} 
						
				smmb.learnModel(instances, rhToFilter, execConfig.getParamFile(), smo.rfo, smo.mbo, smo.scenOpts, adaptiveCapping, srp);
				
				RandomForest rf = smmb.getPreparedForest();
				
				int[] tree_indxs_used = new int[10];
				for(int i=0; i < smo.rfo.numTrees; i++)
				{
					tree_indxs_used[i]= i;
				}
				
				double[][] Theta = new double[1][];
				
			
				double bestMean = Double.POSITIVE_INFINITY;
				
				for(ParamConfiguration config : maxConfigs)
				{
					Theta[0] = config.toValueArray();
					
					double[][] ypred = RandomForest.applyMarginal(rf, tree_indxs_used, Theta);
					
					log.debug("Incumbent {} has predicted mean {}", config, ypred[0]);
					if(ypred[0][0] < bestMean)
					{
						newIncumbent = config;
						bestMean = ypred[0][0];
					}
					
					
				}
				
				log.info("New incumbent selected from random forest prediction is {} with string \"{}\" ", newIncumbent, newIncumbent.getFormattedParamString(StringFormat.NODB_SYNTAX));
				Set<ProblemInstanceSeedPair> maxSet = new HashSet<ProblemInstanceSeedPair>();
				
				maxSet.addAll(rhToFilter.getAlgorithmInstanceSeedPairsRan(newIncumbent));
				
				rhToSaveToDisk = new ThreadSafeRunHistoryWrapper(new NewRunHistory(smo.scenOpts.intraInstanceObj, smo.scenOpts.interInstanceObj, smo.scenOpts.runObj));
				
				
				for(RunData rd : rhToFilter.getAlgorithmRunData())
				{
					while(rd.getIteration() > rhToSaveToDisk.getIteration())
					{
						rhToSaveToDisk.incrementIteration();
					}
					
					
					if(maxSet.contains(rd.getRun().getRunConfig().getProblemInstanceSeedPair()))
					{
						try {
							rhToSaveToDisk.append(rd.getRun());
						} catch (DuplicateRunException e) {
							throw new DeveloperMadeABooBooException("All the runs are coming from a run history object so this really shouldn't happen");
						}
					} else
					{
						log.debug("No match for pisp {}", rd.getRun().getRunConfig().getProblemInstanceSeedPair());
					}
					
				}
				
			} else
			{
				rhToSaveToDisk = rhToFilter;
			}
			int originalRestored = rdi;
			rdi=0;
			for(RunData rd : rhToSaveToDisk.getAlgorithmRunData())
			{
				rdi++;
				log.debug("Will Save Run Iteration {} => {} ", rd.getIteration(), rd.getRun());
			}
			
			
			log.info("Restored Runs Count {} out of {} runs found  ", rdi, originalRestored );
			
			
			
			
			
			
			
			List<ProblemInstance> pisToSave = new ArrayList<ProblemInstance>();
			for(Entry<String, ProblemInstance> ent : fixedPi.entrySet())
			{
				log.info("Problem instance saving {}", ent.getValue());
				pisToSave.add(ent.getValue());
			}
		
			saveState(smo.scenOpts.outputDirectory, rhToSaveToDisk, pisToSave, execConfig.getParamFile().getParamFileName(), execConfig, smo.scenOpts, newIncumbent);
			
		} catch(ParameterException e)
		{
			
			log.info("Error {}", e.getMessage());
			log.debug("Exception ", e);
		} catch(RuntimeException e)
		{
			log.error("Unknown Runtime Exception ",e);
			
		} catch (IOException e) {
			log.error("IO Exception occurred", e);
		}
	}


	
	
	/**
	 * This takes the previously computed runsPerIteration and creates an identical map but with instance ids fixed
	 * @param runsPerIteration MapList, with the runs broken down by iteration
	 * @param fixedPi Map will be populated with new Problem instance objects
	 * @return new map list with the correct problem instance objects.
	 */
	private static MapList<Integer, AlgorithmRun> repairProblemInstances(
			MapList<Integer, AlgorithmRun> runsPerIteration,
			Map<String, ProblemInstance> fixedPi) {
		
		MapList<Integer, AlgorithmRun> repairedRuns = new MapList<Integer, AlgorithmRun>(new HashMap<Integer, List<AlgorithmRun>>());
		int instanceId = 1;
		Set<String> featureKeys = new HashSet<String>();
		ProblemInstance firstPi = null;
		
		
		
		boolean idcollision = false;
outerLoop:
		for(Entry<Integer, List<AlgorithmRun>> runsForIt: runsPerIteration.entrySet())
		{

			int maxIDFound = -1;
			Map<Integer, String> piIDMap = new HashMap<Integer, String>();
			for(AlgorithmRun run : runsForIt.getValue())
			{
				ProblemInstance pi = run.getRunConfig().getProblemInstanceSeedPair().getInstance();
				
				if(piIDMap.containsKey(pi.getInstanceID()))
				{
					
					if(piIDMap.get(pi.getInstanceID()).equals(pi.getInstanceName()))
					{
						continue;
					}
					idcollision = true;
					log.info("Problem Instance ID collision detected, this generally means you are merging run history files over different instance distributions. This is OK but note that instance IDs need to be changed, and so you cannot use this runhistory file to warm start a SMAC run.");
					log.debug("Instance ID: {} previously mapped to: {} but now maps to: {}", pi.getInstanceID(),  piIDMap.get(pi.getInstanceID()), pi.getInstanceName());
					fixedPi.clear();
					break outerLoop;
				} else
				{
					piIDMap.put(pi.getInstanceID(), pi.getInstanceName());
					fixedPi.put(pi.getInstanceName(), pi);
				}

			}
			
		}
		
		if(!idcollision)
		{
			log.info("Problem IDs seem to come from one distribution, this state-merge file should be compatible with warm-starts.");
		}
		
		for(Entry<Integer,List<AlgorithmRun>> runsForIt : runsPerIteration.entrySet())
		{
			for(AlgorithmRun run: runsForIt.getValue())
			{
				ProblemInstance pi =  run.getRunConfig().getProblemInstanceSeedPair().getInstance();
				
				ProblemInstance repairedPi;
				if(fixedPi.containsKey(pi.getInstanceName()))
				{
					
					repairedPi = fixedPi.get(pi.getInstanceName());
				} else
				{
					repairedPi = new ProblemInstance(pi.getInstanceName(), instanceId, pi.getFeatures(), pi.getInstanceSpecificInformation());
					log.debug("Original problem instance {} has ID transformed from {} to {}", pi.getInstanceID(), instanceId);
					fixedPi.put(pi.getInstanceName(), repairedPi);
					
					if(featureKeys.isEmpty())
					{
						featureKeys.addAll(pi.getFeatures().keySet());
						firstPi = pi;
					} else
					{
						if(!featureKeys.equals(pi.getFeatures().keySet()))
						{
							
							String prevMinusCurr = "";
							{
								Set<String> previousFeatures = new HashSet<String>(featureKeys);
								Set<String> currentFeatures = new HashSet<String>(pi.getFeatures().keySet());
								previousFeatures.removeAll(currentFeatures);
								prevMinusCurr = previousFeatures.toString();
							}
							
							String currMinusPrev = "";
							{
								Set<String> previousFeatures = new HashSet<String>(featureKeys);
								Set<String> currentFeatures = new HashSet<String>(pi.getFeatures().keySet());
								currentFeatures.removeAll(previousFeatures);
								currMinusPrev = currentFeatures.toString();
							}
						throw new ParameterException("Feature mismatch exception, features the current instance " + pi.getInstanceName() + " has but we previously on instance "+ firstPi.getInstanceName() +"  didn't find: " + currMinusPrev + " . Features the previous instance has but current instance doesn't: " + prevMinusCurr);
						}
					}
					
					instanceId++;
				}
				
				ProblemInstanceSeedPair newPisp = new ProblemInstanceSeedPair(repairedPi, run.getRunConfig().getProblemInstanceSeedPair().getSeed());
				RunConfig rc = new RunConfig(newPisp, run.getRunConfig().getCutoffTime(), run.getRunConfig().getParamConfiguration());
				
				ExistingAlgorithmRun repairedRun = new ExistingAlgorithmRun(run.getExecutionConfig(), rc, run.getRunResult(), run.getRuntime(), run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

				Object[] args2 = { runsForIt.getKey(), run.getRunConfig().getProblemInstanceSeedPair().getInstance(), run, repairedPi, repairedRun };
				log.debug("Run Restored on iteration {} : {} => {} repaired: {} => {}",args2);
				repairedRuns.addToList(runsForIt.getKey(), repairedRun);
				
			}
						
		}
		
		return repairedRuns;
	}


	/**
	 * Extract the runs from a given directory and add them to the runsPerIterationMap
	 * 
	 * @param smo options object
	 * @param pis instances
	 * @param execConfig execConfig object
	 * @param runsPerIteration MapList containing the runs for each iteration
	 * @param dir	directory with valid state information
	 * @throws IOException
	 */
	private static void extractRunsFromDirectory(StateMergeOptions smo,
			List<ProblemInstance> pis, AlgorithmExecutionConfig execConfig,
			MapList<Integer, AlgorithmRun> runsPerIteration, String dir)
			throws IOException {
		ThreadSafeRunHistory rh = new ThreadSafeRunHistoryWrapper(new NewRunHistory(smo.scenOpts.intraInstanceObj, smo.scenOpts.interInstanceObj, smo.scenOpts.runObj));
		restoreState(dir, smo.scenOpts, pis, execConfig, rh, smo.restoreScenarioArguments);
		
		log.debug("Restored state of {} has {} runs for default configuration ", dir, rh.getAlgorithmRunData(execConfig.getParamFile().getDefaultConfiguration()).size());
		double restoredRuntime = 0.0;
		for(RunData rd : rh.getAlgorithmRunData())
		{
			restoredRuntime+=rd.getRun().getRuntime();
			
			if(rd.getIteration() > smo.iterationLimit)
			{
				break;
			}
			runsPerIteration.addToList(rd.getIteration(), rd.getRun());
			
			if(restoredRuntime > smo.tunerTime)
			{
				break;
			}
			
		}
	}


	/**
	 * For each directory option, finds the directories to restore and returns a set containing all of them
	 * @param directories 
	 * @return set of directories with state data to restore
	 */
	private static Set<String> getAllRestoreDirectories(List<String> directories) {
		Set<String> directoriesWithState = new HashSet<String>();
		log.info("Beginning Directory Scan");
		for(String dir: directories)
		{
			 directoriesWithState.addAll(scanDirectories(dir));
		}
		return directoriesWithState;
	}

	
	private static void saveState(String dir, ThreadSafeRunHistory rh, List<ProblemInstance> pis, String configSpaceFileName, AlgorithmExecutionConfig execConfig, ScenarioOptions scenOpts, ParamConfiguration newIncumbent) throws IOException 
	{
		
		//StateFactoryOptions sfo = new StateFactoryOptions();
		
		log.debug("Saving directory {}", dir);
		
		LegacyStateFactory lsf = new LegacyStateFactory(dir,null);
		
	
		StateSerializer ss = lsf.getStateSerializer("it", rh.getIteration());
		
		ss.setRunHistory(rh);
		
		StringBuilder scen = new StringBuilder();
		
		
		
		
		scen.append("# Automatically generated by State Merge Utility").append("\n");
		scen.append("algo="+scenOpts.algoExecOptions.algoExec).append("\n");
		scen.append("execdir="+scenOpts.algoExecOptions.algoExecDir).append("\n");
		scen.append("deterministic=" + scenOpts.algoExecOptions.deterministic).append("\n");
		scen.append("run_obj=" + scenOpts.runObj.toString().toLowerCase()).append("\n");
		scen.append("#outdir = (Outdir is not recommended in a scenario file anymore)").append("\n");
		scen.append("overall_obj=" + scenOpts.intraInstanceObj.toString().toLowerCase()).append("\n");
		scen.append("cutoff_time=" + execConfig.getAlgorithmCutoffTime()).append("\n");
		scen.append("tunerTimeout=" + scenOpts.limitOptions.tunerTimeout).append("\n");
		scen.append("paramfile=" + LegacyStateFactory.PARAM_FILE).append("\n");
		scen.append("instance_file=" + LegacyStateFactory.INSTANCE_FILE).append("\n");
		if(pis.get(0).getFeatures().size() > 0)
		{
			scen.append("feature_file=" + LegacyStateFactory.FEATURE_FILE).append("\n");
		}
		
		
		lsf.copyFileToStateDir(LegacyStateFactory.SCENARIO_FILE, new ReaderInputStream(new StringReader(scen.toString()),"UTF-8"));
		
		
		
		if(pis.get(0).getFeatures().size() > 0)
		{
		
			StringBuilder features = new StringBuilder();
			features.append(",");
			for(String key : pis.get(0).getFeatures().keySet())
			{
				features.append(key).append(",");
			}
			features.setCharAt(features.length()-1, '\n');
			
			for(ProblemInstance pi : pis)
			{
				features.append(pi.getInstanceName()).append(",");
				for(Entry<String, Double> ent : pi.getFeatures().entrySet())
				{
					features.append(ent.getValue()).append(",");
				}
				features.setCharAt(features.length()-1, '\n');
			}
			lsf.copyFileToStateDir(LegacyStateFactory.FEATURE_FILE, new ReaderInputStream(new StringReader(features.toString()),"UTF-8"));
		}
		
		StringBuilder piTxt = new StringBuilder();
		
		for(ProblemInstance pi : pis)
		{
			piTxt.append(pi.getInstanceName()).append("\n");
		}
		
		lsf.copyFileToStateDir(LegacyStateFactory.INSTANCE_FILE, new ReaderInputStream(new StringReader(piTxt.toString()),"UTF-8"));
		lsf.copyFileToStateDir(LegacyStateFactory.PARAM_FILE, new File(configSpaceFileName));
		ss.setIncumbent(newIncumbent);
		
		ss.save();
	}

	private static void restoreState(String dir, ScenarioOptions scenOpts, List<ProblemInstance> pis, AlgorithmExecutionConfig execConfig, ThreadSafeRunHistory rh, String restoreScenarioOptions) throws IOException {
		
		StateFactoryOptions sfo = new StateFactoryOptions();
		
		log.debug("Restoring directory {}", dir);
		
		LegacyStateFactory lsf = new LegacyStateFactory(null, dir);
		
	
		for(File f : new File(dir).listFiles())
		{
			if(f.getName().equals(LegacyStateFactory.SCENARIO_FILE))
			{
				log.debug("Using built in scenario options for directory {}", dir);
				scenOpts = new ScenarioOptions();
				
				JCommander jcom = new JCommander(scenOpts);
				
				ArrayList<String> args = new ArrayList<String>();
				args.add("--scenario-file");
				args.add(f.getAbsolutePath());
				
				if(new File(dir + File.separator + LegacyStateFactory.INSTANCE_FILE).exists())
				{
					args.add("--instance-file");
					args.add(dir + File.separator + LegacyStateFactory.INSTANCE_FILE);
				}
				
				if(new File(dir + File.separator +LegacyStateFactory.FEATURE_FILE).exists())
				{
					args.add("--feature-file");
					args.add(dir + File.separator +LegacyStateFactory.FEATURE_FILE);
				} else if(new File(dir + File.separator +"instance-features.txt").exists())
				{
					args.add("--feature-file");
					args.add(dir + File.separator +"instance-features.txt");
				}
				
				args.addAll(Arrays.asList(SplitQuotedString.splitQuotedString(restoreScenarioOptions)));
				jcom.parse(args.toArray(new String[0]));
				
				pis = scenOpts.getTrainingAndTestProblemInstances(dir, 0, 0, true, false, false, false).getTrainingInstances().getInstances();
				
			}
		}
			
		lsf.getStateDeserializer("it", Integer.MAX_VALUE, execConfig.getParamFile(), pis, execConfig, rh);
	}

	/**
	 * Scans directories and returns a list 
	 * @param dir
	 * @return
	 */
	private static Set<String> scanDirectories(String dirStr) {
		File dir = new File(dirStr).getAbsoluteFile();
		
		log.debug("Scanning directory {}", dir.getAbsolutePath());
		if (!dir.exists())
		{
			throw new ParameterException("Argument " + dir.getAbsolutePath()  + " does not exist");
		}
		if (!dir.canRead())
		{
			throw new ParameterException("Argument " + dir.getAbsolutePath()  + " cannot be read");
		}
		if (!dir.isDirectory())
		{
			throw new ParameterException("Argument " + dir.getAbsolutePath()  + " is not a directory");
		}
		
		Set<String> sd= scanDirectories(dir, new HashSet<String>());
		
		if(sd.isEmpty())
		{
			throw new ParameterException("Couldn't find any state files in " + dir.getAbsolutePath());
		} else
		{
			return sd;
		}
		
		
	}

	private static Set<String> scanDirectories(File dir, Set<String> absPathSearched)
	{
		
		if(absPathSearched.contains(dir.getAbsolutePath()))
		{
			return Collections.emptySet();
		} else
		{
			absPathSearched.add(dir.getAbsolutePath());
		}
		String s2 = LegacyStateFactory.getRunAndResultsFilename("", "(-it\\d+|)","","");
		
		Set<String> matchingDirectories = new HashSet<String>();
		for(String s : dir.list())
		{
			
			if(s.matches(s2))
			{
				log.debug("Directory contains saved SMAC data {}", dir);
				matchingDirectories.add(dir.getAbsolutePath());
			} 	
		}
		
		if(!matchingDirectories.isEmpty())
		{
			return matchingDirectories;
		}
		
		
		for(File f : dir.listFiles())
		{
			if(f.isDirectory() && f.canRead())
			{
				matchingDirectories.addAll(scanDirectories(f, absPathSearched));
			}
		}
		
		return matchingDirectories;
	}
}

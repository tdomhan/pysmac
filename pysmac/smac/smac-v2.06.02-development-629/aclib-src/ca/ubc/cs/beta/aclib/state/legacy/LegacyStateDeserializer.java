package ca.ubc.cs.beta.aclib.state.legacy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.exceptions.StateSerializationException;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
import ca.ubc.cs.beta.aclib.state.StateDeserializer;

/**
 * Supports deserializing the state from the files saved by the LegacyStateSerializer instance,
 * it should also support restoring from MATLAB save files.
 * 
 * See the interface for more details on how this interface works or why it does what it does.
 * 
 * Note: We use the paramstrings file to restore state and not the uniq_configurations since the strings
 * are more robust, this unlike matlab which may only return uniq_configurations.
 * 
 * @author seramage
 *
 */
public class LegacyStateDeserializer implements StateDeserializer {

	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * RunHistory object that we restored
	 */
	private final RunHistory runHistory;
	
	
	/**
	 * Iteration that we restored
	 */
	private final int iteration;
	
	/**
	 * Incumbent that we restored
	 */
	private final ParamConfiguration incumbent;
	
	/**
	 * When we get a duplicate run exception we start generating seeds from this number and keep incrementing
	 * This occurs primarily when building surrogates 
	 */
	
	private static int newSeeds = 1024;
	
	/**
	 * Stores whether or not we were able to recover a complete state 
	 */
	private final boolean incompleteSavedState;
	
	/**
	 * Stores the index where we expect the run iteration to be.
	 * This is also used when autodetecting the iterations we can restore
	 */
	public static final int RUN_ITERATION_INDEX = 11;
	
	
	public final double cutoffTime;
	
	private ParamConfiguration potentialIncumbent;
	private final  Map<String, Serializable> objectStateMap; 
	
	
	private static final Pattern filePattern = Pattern.compile("it(\\d+).");
	@SuppressWarnings("unchecked")
	/**
	 * Generates objects necessary to restore SMAC to the state in the saved files
	 * 
	 * Generally when restoring state you should always use the same values as the run was generated
	 * changing certain values may result in weird behaivor (for instance changing the runObjective, is almost non-sensical)
	 * 
	 * @param restoreFromPath 				Directory to restore from
	 * @param id							The id to restore (generally this is "it" but maybe "CRASH")
	 * @param iteration						The iteration to restore
	 * @param configSpace					The configuration space the state files are from
	 * @param instances						The instances used in the run
	 * @param execConfig					The execution configuration used in the run
	 * @param emptyRunHistory				A RunHistory object that has no runs in it
	 * @throws StateSerializationException  If we cannot restore the state
	 */
	LegacyStateDeserializer(String restoreFromPath, String id, int iteration, ParamConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfig execConfig, RunHistory emptyRunHistory) 
	{
			if (configSpace == null) throw new IllegalArgumentException("Config Space cannot be null");
			if(emptyRunHistory == null) throw new IllegalArgumentException("Run History cannot be null");
			if(emptyRunHistory.getAlgorithmRunData().size() > 0)
			{
				log.warn("RunHistory object already contains runs this may cause problems restoring runs");
			}
			this.runHistory = emptyRunHistory;
			if(instances == null) throw new IllegalArgumentException("Instances cannot be null");
			
			if(execConfig == null) throw new IllegalArgumentException("execConfig cannot be null");
			this.cutoffTime = execConfig.getAlgorithmCutoffTime();
			
			if(instances.size() == 0) 
			{
				log.warn("Got empty instance list, except in the trivial case this will result in an exception");
			}
			
			if (iteration == Integer.MAX_VALUE)
			{
				iteration = 0;
				log.info("Auto detecting iteration for directory {}" , restoreFromPath);
				
				File restoreDirectory = new File(restoreFromPath);
				File[] files = restoreDirectory.listFiles();
				Arrays.sort(files);
				for(File f : files)
				{
					int thisIteration = LegacyStateFactory.readIterationFromObjectFile(f);
					if(thisIteration != -1)
					{
						
						
						//System.out.println(f.getAbsolutePath().substring(restoreDirectory.getAbsolutePath().length()+1) + " => Iteration " + thisIteration);
						
						iteration = Math.max(thisIteration, iteration);
					}
				}
				
				if(iteration == 0)
				{
					log.debug("Auto-detected iteration 0 on first pass trying by filename, doing another pass");
					
					for(File f : files)
					{

						if(f.getName().matches(LegacyStateFactory.getRunAndResultsFilename("", "it", "\\d+")))
						{
							
							Matcher m = filePattern.matcher(f.getName());
							
							if(m.find())
							{
								String group = m.group(1);
								log.debug("Found iteration {} from file {} ", group , f.getName());
								iteration = Math.max(Integer.valueOf(group), iteration);
							}
							
						}
						
						
						
					}
				}
				log.info("Iteration restoring to {} ", iteration);
			}
			
			
			Object[] args = { iteration, id, restoreFromPath };
			log.info("Trying to restore iteration: {} id: {} from path: {}", args );
			try {
				
				
			FileLocations f = getLocations(restoreFromPath, id, iteration);
			f.validate();
			
			log.trace("Run and Results File: {}", f.runHistoryFile.getAbsolutePath());
			if(f.paramStringsFile != null)
			{
				log.trace("Param Strings File: {}", f.paramStringsFile.getAbsolutePath());
			} 
			
			if(f.uniqConfigFile != null)
			{
				log.trace("Param Strings File: {}", f.uniqConfigFile.getAbsolutePath());
			}
				
			if(f.javaObjDumpFile != null)
			{
				log.trace("Java Objective Dump File: {}",f.javaObjDumpFile.getAbsolutePath());
			} else
			{
				log.trace("Java Objective Dump File: null");
			}
			
			if(f.javaObjDumpFile != null)
			{
				incompleteSavedState = false;
				/**
				 * Java Object
				 */
				ObjectInputStream oReader =  null;
				try {
					oReader =  new ObjectInputStream(new FileInputStream(f.javaObjDumpFile));

					Map<String, Serializable> objectMap = (Map<String, Serializable>) oReader.readObject();
					

					
					int storedIteration =(Integer) objectMap.get(LegacyStateFactory.ITERATION_KEY);
					if(storedIteration != iteration) 
					{
						
						throw new IllegalStateException("File Found claimed to be for iteration " + iteration + " but contained iteration " + storedIteration + " in file: " + f.javaObjDumpFile.getAbsolutePath());
					}
					
					this.iteration = storedIteration;
					

					
					String paramString = (String) objectMap.get(LegacyStateFactory.INCUMBENT_TEXT_KEY);
					
					if(paramString != null)
					{
						incumbent = configSpace.getConfigurationFromString(paramString, StringFormat.STATEFILE_SYNTAX);
					} else
					{
						//throw new IllegalStateException("Not sure why a java object file has no incumbent, save state file corrupt. To continue try renaming the object file for this iteration");
						incumbent = null;
					}
					 
					this.objectStateMap = (Map<String, Serializable>) objectMap.get(LegacyStateFactory.OBJECT_MAP_KEY);
				} finally
				{
					if(oReader != null) oReader.close(); 
				}
			} else
			{
				incompleteSavedState = true;
				
				
				
				
				
				this.iteration = iteration;
				
				this.objectStateMap = Collections.EMPTY_MAP;
				
			
				this.incumbent = null;
			
			}
			
			
			

			
			/**
			 * Get Param Strings
			 */
			BufferedReader reader = null;
			
			Map<Integer,ParamConfiguration> configMap = new HashMap<Integer, ParamConfiguration>();
			
			try {
				
					if(f.uniqConfigFile != null)
					{
						log.info("Parsing Parameter Settings from: {}", f.uniqConfigFile.getAbsolutePath());
						log.info("Parameter Settings specified in array format, which is less portable (but more accurate) than paramString format");
						reader =  new BufferedReader(new FileReader(f.uniqConfigFile));
						
						String line = null;
					
						while( (line = reader.readLine()) != null)
						{
							log.trace("Parsing config line: {}", line);
							String[] lineResults = line.split(",",2);
							
							if(lineResults.length != 2) throw new IllegalArgumentException("Configuration Param Strings File is corrupted, no comma detected on line: \"" + line + "\"");
							
							Integer configId = Integer.valueOf(lineResults[0]);

							configMap.put(configId,configSpace.getConfigurationFromString(lineResults[1], StringFormat.ARRAY_STRING_SYNTAX));
							
							
						}
						
						
					} else if(f.paramStringsFile != null)
					{
						log.info("Parsing Parameter Settings from: {}",f.paramStringsFile.getAbsolutePath());
					
						reader =  new BufferedReader(new FileReader(f.paramStringsFile));
						
						String line = null;
					
						while( (line = reader.readLine()) != null)
						{
							log.trace("Parsing config line: {}", line);
							String[] lineResults = line.split(":",2);
							
							if(lineResults.length != 2) throw new IllegalArgumentException("Configuration Param Strings File is corrupted, no colon detected on line: \"" + line + "\"");
							
							Integer configId = Integer.valueOf(lineResults[0]);
							configMap.put(configId,configSpace.getConfigurationFromString(lineResults[1], StringFormat.STATEFILE_SYNTAX));
							
							
						} 
					} else
					{
						throw new IllegalStateException("One of Unique Configuration File, or Param Strings File should have been non-null, cannot continue");
					}
				
				
			} finally
			{
				if (reader != null) reader.close();
			}
			
			
			/**
			 * Create map of InstanceID to instance
			 */
			Map<Integer, ProblemInstance> instanceMap = new HashMap<Integer, ProblemInstance>();
			for(ProblemInstance instance : instances)
			{
				instanceMap.put(instance.getInstanceID(), instance);
			}
			
			
			/**
			 * Create Run History Object
			 */
			
			//runHistory = new NewRunHistory(instanceSeedGenerator, intraInstanceObjective, interInstanceObjective, runObj);
			
			
			CSVReader runlist = null;
			try {
					runlist = new CSVReader( new FileReader(f.runHistoryFile));
			
				String[] runHistoryLine = null;
				int i=0;
				
				boolean seedErrorLogged = false;
				
				int duplicateRunsDropped = 0;
				while((runHistoryLine = runlist.readNext()) != null)
				{
					
					
					
					try {
						//The magic constants here are basically from LegacyStateSerializer
						//Perhaps that should be refactored as this is fragile
						
						//Don't need cumulative sum NOR run number (12 and 0).
						//Columns 13 (Run Result) and 14 (Algorithm Meta Data) may not appear
						
						
						
						if(runHistoryLine[0].trim().equals(LegacyStateFactory.RUN_NUMBER_HEADING)) continue;
						i++;
						
						
						int thetaIdx = Integer.valueOf(runHistoryLine[1]);
						
						int instanceIdx = Integer.valueOf(runHistoryLine[2]);
						
						
						ProblemInstance pi = instanceMap.get(instanceIdx);
						if(pi == null)
						{
							throw new StateSerializationException("Run History file referenced a Instance ID that does not exist (Column 3) on line: " + Arrays.toString(runHistoryLine) + " we know about " + instances.size() + " instances" );
						}
						
						
						// We don't really care what this says, so we don't validate it.
						//double y = Double.valueOf(runHistoryLine[3]);
						
						
						boolean isCensored = ((runHistoryLine[4].trim().equals("0") ? false : true));
						
						double cutOffTime = Double.valueOf(runHistoryLine[5]);

						//Try to parse the seed as a long, MATLAB stores seeds as double and these are lossy.
						//Infinite and NaN seeds are just invalid
						long seed = -1;
						try {
							seed = Long.valueOf(runHistoryLine[6]);
						
						} catch(NumberFormatException e)
						{
							seed = Double.valueOf(runHistoryLine[6]).longValue();
							
							if(Double.isNaN(seed) || Double.isInfinite(seed))
							{
								throw new StateSerializationException("Encountered Illegal Seed Value (either Infinite or Nan) (Column 7) in line: " + Arrays.toString(runHistoryLine));
							}
							
							if(!seedErrorLogged)
							{
								log.warn("Seed value specified in imprecise format on line {}  contents: {}", i,  runHistoryLine);
								seedErrorLogged = true;
							}
							
						}
						
					
						//Handles some MATLAB Compatibility issues
						double runtime = Double.valueOf(runHistoryLine[7].trim().replaceAll("Inf$", "Infinity"));
						if(Double.isNaN(runtime) || Double.isInfinite(runtime))
						{
							throw new StateSerializationException("Encountered an Illegal Runtime value (Infinity or NaN) (Column 8) on line: " + Arrays.toString(runHistoryLine));
						}
						
						
						double runLength = -1;						
						try {
							runLength = Double.valueOf(runHistoryLine[8].trim().replaceAll("Inf$", "Infinity"));
						} catch(NumberFormatException e)
						{
						
							throw new StateSerializationException("Encountered an Illegal Runlength Value (Column 9) on line: " + Arrays.toString(runHistoryLine));
						}
						
						
						RunResult runResult;
						if(runHistoryLine.length >= 14 )
						{
							//Index 13 should exist which stores a nicer format
							runResult = RunResult.getAutomaticConfiguratorResultForKey(runHistoryLine[13]);
						} else
						{
							 runResult = RunResult.getAutomaticConfiguratorResultForCode(Integer.valueOf(runHistoryLine[9]));
						}
						
					
						String additionalRunData = "";
						if(runHistoryLine.length >= 15)
						{
							additionalRunData = runHistoryLine[14].trim();
						}
						
						
						double wallClockTime = 0.0;
						if(runHistoryLine.length >= 16)
						{
							wallClockTime = Double.valueOf(runHistoryLine[15].trim());
						}
						double quality =  (double) Double.valueOf(runHistoryLine[10].trim().replaceAll("Inf$", "Infinity"));
						int runIteration = Integer.valueOf(runHistoryLine[LegacyStateDeserializer.RUN_ITERATION_INDEX]);

						if(runIteration > iteration) break;
						if(runIteration < runHistory.getIteration())
						{
						
							log.warn("Out of order run detected in line {} (Column 11), current iteration is: {}, this may be a corrupt file, or MATLAB deleting previously capped runs ", Arrays.toString(runHistoryLine), runHistory.getIteration());
							runIteration = runHistory.getIteration();
						}
						while(runIteration > runHistory.getIteration())
						{
							runHistory.incrementIteration();
						}
						
						
						if(runtime > execConfig.getAlgorithmCutoffTime())
						{
							runtime = execConfig.getAlgorithmCutoffTime();
							runResult = RunResult.TIMEOUT;
							cutOffTime = runtime;
							log.info("Cutoff time discrepancy detected while restoring state for line {}, marking run as TIMEOUT with runtime {}", Arrays.toString(runHistoryLine), runtime);
							
							
						} else if (runtime < execConfig.getAlgorithmCutoffTime())
						{
							if(runResult.equals(RunResult.TIMEOUT) && !isCensored)
							{
								
								log.info("Cutoff time discrepancy detected while restoring state for line {}, marking run as TIMEOUT and Censored with runtime {}", Arrays.toString(runHistoryLine), runtime);
								isCensored = true;
								
								
							}
							
							
						}
						
						
					
						ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, seed); 
						RunConfig runConfig = new RunConfig(pisp, cutOffTime, configMap.get(thetaIdx),isCensored);
												
						AlgorithmRun run = new ExistingAlgorithmRun(execConfig, runConfig, runResult, runtime, runLength, quality, seed, additionalRunData, wallClockTime);
						
						log.trace("Appending new run to runHistory: ", run);
						try {
							runHistory.append(run);
						} catch (DuplicateRunException e) {

							
							
							if(seed == -1)
							{
								//This is for Model Building and is probably a bug for restoring state later
								
								log.trace("Seed is -1 which means it was deterministic, logging run with a new seed");
								
								seed = newSeeds++;
								

								run = new ExistingAlgorithmRun(execConfig, runConfig, runResult, runtime, runLength, quality, seed, wallClockTime);
								
								log.trace("Appending new run to runHistory: ", run);
								try {
									runHistory.append(run);
								} catch(DuplicateRunException e2)
								{
									log.info("Could not restore run, duplicate run detected again for deterministic run: {} on line {} ", run, Arrays.toString(runHistoryLine));
								}
									
								
								
							} else
							{
								duplicateRunsDropped++;
								log.debug("Duplicate Run Detected dropped {} from line: {}",run, Arrays.toString(runHistoryLine));
								
							}
							
						}
					} catch(StateSerializationException e)
					{
						 throw e;
					} catch(RuntimeException e) 
					{
						throw new StateSerializationException("Error occured while parsing the following line of the runHistory file: " + i + " data "+ Arrays.toString(runHistoryLine), e);
					}

				}
				
				log.info("Restored {} runs, {} duplicates were dropped", i, duplicateRunsDropped);
				if(this.incumbent == null)
				{
					log.info("No incumbent found in state files, doing our best to select the incumbent, may not be identical but should be indistinguishable");
					
					RunHistory runHistory = getRunHistory();	
					Set<ParamConfiguration> configs = runHistory.getUniqueParamConfigurations();
					Set<ParamConfiguration> possibleIncumbents = new HashSet<ParamConfiguration>();
					int maxPISPS = 0;
					for(ParamConfiguration config : configs)
					{
						int configPISPCount = runHistory.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
						if(maxPISPS < configPISPCount )
						{
							maxPISPS =  configPISPCount;
							possibleIncumbents.clear();
							possibleIncumbents.add(config);
						} else if(maxPISPS == configPISPCount)
						{
							possibleIncumbents.add(config);
						}
					}
					
					double minEmpiricalCost = Double.POSITIVE_INFINITY;
					Set<ProblemInstance> instancesRan = runHistory.getUniqueInstancesRan();
					ParamConfiguration potentialIncumbent = null;
					
					for(ParamConfiguration config : possibleIncumbents)
					{
						double thisEmpiricalCost = runHistory.getEmpiricalCost(config, instancesRan, cutoffTime);
						
						if(thisEmpiricalCost < minEmpiricalCost)
						{
							minEmpiricalCost = thisEmpiricalCost;
							potentialIncumbent = config;
						}
					}
					
					this.potentialIncumbent = potentialIncumbent;
				} else
				{
					this.potentialIncumbent = null;
				}
			
			} finally
			{
				if(runlist != null) runlist.close();
			}
			
			
			
			
		} catch(IOException e)
		{
			throw new StateSerializationException("Could not restore state", e);
		} catch (ClassNotFoundException e) {
			throw new StateSerializationException("Java Serialization Failed", e);
		}
		
		
		log.info("Successfully restored iteration: {} id: {} from path: {}", args );
			
	}

	
	@Override
	public RunHistory getRunHistory() {

		return runHistory;
	}

	/**
	 * Determines which files we should use to read the data out of the state directory
	 * 
	 * 
	 * @param path		 string containing the directory to restore
	 * @param id		 the id that we are restoring
	 * @param iteration	 the iteration that we are restoring
	 * @return	filelocations object with the files we should use
	 * @throws StateSerializationException if we cannot determine the necessary files
	 */
	private FileLocations getLocations(String path, String id, int iteration)
	{
		/*
		 * Restore algorithm is as follows:
		 * Find a runHistory and paramString containing the iteration data
		 * Find our current java object dump for our rand object.
		 * If it doesn't contain the instanceSeedFile, then load the instance Seed file from base version and replay
		 */
		FileLocations f = new FileLocations();
		File restoreDirectory = new File(path);
	
		Set<String> filenames = new HashSet<String>();
		
		if(!restoreDirectory.exists()) throw new IllegalArgumentException("Restore Directory specified: " + path + " does not exist");
		filenames.addAll(Arrays.asList(restoreDirectory.list()));
		int savedFileIteration = 0;
		boolean filesFound = false;
		
		int maxIteration = getMaxIterationInDirectory(restoreDirectory, id);
		
	
		for(savedFileIteration=iteration; savedFileIteration <= 2*maxIteration; savedFileIteration++ )
		{ //We scan for the closest, because MATLAB generally overwrites previous runs in place.
			
			if(filenames.contains(LegacyStateFactory.getRunAndResultsFilename("", id, savedFileIteration)))
			{
				if(filenames.contains(LegacyStateFactory.getParamStringsFilename("", id, savedFileIteration)))
				{
					f.runHistoryFile = new File(LegacyStateFactory.getRunAndResultsFilename(path, id, savedFileIteration));
					f.paramStringsFile = new File(LegacyStateFactory.getParamStringsFilename(path, id, savedFileIteration));
					f.uniqConfigFile = new File(LegacyStateFactory.getUniqConfigurationsFilename(path, id, savedFileIteration));
					
					if(!f.uniqConfigFile.exists())
					{
						f.uniqConfigFile = null;
					}
					filesFound = true;
					break;
				} else
				{
					log.warn("Didn't find paramStrings file: {} but did find Run and Results file: {}, it's possible saved state directory structure is corrupted.", LegacyStateFactory.getParamStringsFilename(path, id, savedFileIteration), LegacyStateFactory.getRunAndResultsFilename(path, id, savedFileIteration) );
					continue;
				}
			}
			
			if(filenames.contains(LegacyStateFactory.getParamStringsFilename("", "CRASH", savedFileIteration)))
			{
				if(filenames.contains(LegacyStateFactory.getRunAndResultsFilename("", "CRASH", savedFileIteration)))
				{
					
					filesFound = true;
					f.runHistoryFile = new File(LegacyStateFactory.getRunAndResultsFilename(path, "CRASH", savedFileIteration));
					f.paramStringsFile = new File(LegacyStateFactory.getParamStringsFilename(path, "CRASH", savedFileIteration));

					break;
				} else
				{
					log.warn("Found paramStrings file: {} but no Run and Results file: {}, it's possible saved state directory structure is corrupted.", LegacyStateFactory.getUniqConfigurationsFilename(path, id, savedFileIteration), LegacyStateFactory.getParamStringsFilename(path, id, savedFileIteration) );
					continue;
				}
			}
			
			
			
			
			
			
		}
		
		
		if(!filesFound)
		{
			if(filenames.contains(LegacyStateFactory.getRunAndResultsFilename("", "", "","")))
			{
				f.runHistoryFile = new File(LegacyStateFactory.getRunAndResultsFilename(path, "", "",""));
				if(filenames.contains(LegacyStateFactory.getParamStringsFilename("", "", "","")))
				{
					
					f.paramStringsFile = new File(LegacyStateFactory.getParamStringsFilename(path, "", "",""));

					filesFound = true;
					
				} else if(filenames.contains(LegacyStateFactory.getUniqConfigurationsFilename("", "", "", "")))
				{
					f.uniqConfigFile = new File(LegacyStateFactory.getUniqConfigurationsFilename(path, "","",""));
					filesFound = true;
				} else
					
				{ 
					
					log.warn("Didn't find paramStrings file: {} but did find Run and Results file: {}, it's possible saved state directory structure is corrupted.", LegacyStateFactory.getParamStringsFilename(path, id, savedFileIteration),f.runHistoryFile.getAbsolutePath() );
					f.runHistoryFile = null;
					
				}
			}
			
			
		}
			
			
		if(!filesFound)
		{
			throw new StateSerializationException("Could not find data files to restore iteration " + iteration + " with id " + id + " in path " + path );
		}
		
		
		
		
		f.javaObjDumpFile = new File(LegacyStateFactory.getJavaObjectDumpFilename(path, id, iteration));
		
		if(!f.javaObjDumpFile.exists())
		{
			log.info("Could not find object dump file to restore from {}",f.javaObjDumpFile.getAbsolutePath());
			f.javaObjDumpFile = new File(LegacyStateFactory.getJavaQuickBackObjectDumpFilename(path, id, iteration));
			if(LegacyStateFactory.readIterationFromObjectFile(f.javaObjDumpFile) != iteration)
			{
				log.info("Could not find object dump file to restore from {}",f.javaObjDumpFile.getAbsolutePath());
				f.javaObjDumpFile = new File(LegacyStateFactory.getJavaQuickObjectDumpFilename(path, id, iteration));
				if(LegacyStateFactory.readIterationFromObjectFile(f.javaObjDumpFile) != iteration)
				{
					log.info("Could not find object dump file to restore from {}",f.javaObjDumpFile.getAbsolutePath());
					f.javaObjDumpFile = new File(LegacyStateFactory.getJavaObjectDumpFilename(path, "CRASH", iteration));
					if(LegacyStateFactory.readIterationFromObjectFile(f.javaObjDumpFile) != iteration)
					{
						
						log.info("Could not find object dump file to restore from {}, No Java Object Dump will be loaded",f.javaObjDumpFile.getAbsolutePath());
						f.javaObjDumpFile = null;
						//throw new StateSerializationException("Java Object File does not exist " +  LegacyStateFactory.getJavaQuickObjectDumpFilename(path, id, iteration));
					}
					
				}
						
			}
		}
	 
		return f;
		
	}
	private int getMaxIterationInDirectory(File restoreDirectory, String id) {
		
		String[] subfiles = restoreDirectory.list();
		
		
		Pattern p = Pattern.compile(Pattern.quote(id)+"(\\d+)\\z");
		
		 int maxIteration = 0;
		 
		for(String file : subfiles)
		{
			//Strip file extension
			file = file.trim().substring(0, file.trim().length() - 4);
			Matcher m = p.matcher(file);
			
			while(m.find())
			{
				int myIteration = 0;
				try {
					myIteration = Integer.valueOf(m.group(1));
				} catch(NumberFormatException e)
				{
					log.warn("Weird filename encountered that looked like a integer, but wasn't {}, while autodetecting iteration", file);
					continue;
				}
				if(myIteration > maxIteration)
				{
					maxIteration = myIteration;
				}
			}
			
			
		}
			
			
		return maxIteration;
		
		
	}
	/**
	 * Value Object Class that stores File instances for the files we should restore from
	 * @author sjr
	 *
	 */
	private static class FileLocations
	{
		public File uniqConfigFile;
		public File runHistoryFile;
		public File paramStringsFile;
		public File javaObjDumpFile;
		
		/**
		 * Ensures that all the files exist otherwise throws an exception
		 */
		public void validate()
		{
			if ((runHistoryFile != null) && (!runHistoryFile.exists())) throw new StateSerializationException("Run History File does not exist");
			if ((javaObjDumpFile != null) && (!javaObjDumpFile.exists())) throw new StateSerializationException("Java Object File does not exist");
			
			
			if ((paramStringsFile != null) && (!paramStringsFile.exists())) throw new StateSerializationException("Param Strings File does not exist");
			if ((uniqConfigFile != null) && (!uniqConfigFile.exists())) throw new StateSerializationException("Unique Config File does not exist");
			
		}
	}
	@Override
	public int getIteration() {
		return iteration;
	}


	@Override
	public ParamConfiguration getIncumbent() {
		
		if(incumbent == null && incompleteSavedState)
		{
			return potentialIncumbent;
		} else
		{
			return incumbent;
		}
	}


	@Override
	public Map<String, Serializable> getObjectStateMap() {
		return ((objectStateMap != null) ? objectStateMap : Collections.<String, Serializable>emptyMap());
	}
	
	
	

}

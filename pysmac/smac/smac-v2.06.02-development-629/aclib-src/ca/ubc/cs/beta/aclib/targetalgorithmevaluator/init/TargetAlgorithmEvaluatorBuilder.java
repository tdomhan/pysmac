package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug.CheckForDuplicateRunConfigDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug.LeakingMemoryTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug.LogEveryTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug.RunHashCodeVerifyingAlgorithmEvalutor;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug.UncleanShutdownDetectingTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.TerminateAllRunsOnFileDeleteTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.transform.TransformTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers.KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers.OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers.RetryCrashedRunsTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers.WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.prepostcommand.PrePostCommandTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.AbortOnCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.AbortOnFirstRunCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.SATConsistencyTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.TimingCheckerTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety.VerifySATTargetAlgorithmEvaluator;


public class TargetAlgorithmEvaluatorBuilder {

	private static Logger log = LoggerFactory.getLogger(TargetAlgorithmEvaluatorBuilder.class);
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behavior
	 * 
	 * @param options 		   		Target Algorithm Evaluator Options
	 * @param execConfig	   		Execution configuration for the target algorithm
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param taeOptionsMap			A map that contains mappings between the names of TAEs and their configured options object	
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 * @deprecated Use the non wrapped method
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap)
	{
		return getTargetAlgorithmEvaluator(options, execConfig, hashVerifiersAllowed, taeOptionsMap, null);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 			   	Target Algorithm Evaluator Options
	 * @param execConfig	   		Execution configuration for the target algorithm
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 * @deprecated Use the non wrapped method
	 */
	@Deprecated
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae)
	{
		return getTargetAlgorithmEvaluator(options,execConfig, hashVerifiersAllowed, false, taeOptionsMap, tae);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param execConfig	   Execution configuration for the target algorithm
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param ignoreBound	   Whether to ignore bound requests
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae)
	{
		return getTargetAlgorithmEvaluator(options, execConfig, hashVerifiersAllowed, ignoreBound, taeOptionsMap, tae, new File("."), 0);
	}
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param execConfig	   Execution configuration for the target algorithm
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param ignoreBound	   Whether to ignore bound requests
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae, File outputDir, int numRun)
	{
		
		if(taeOptionsMap == null)
		{
			throw new IllegalArgumentException("taeOptionsMap must be non-null and contain the option objects for all target algorithm evaluators");
		}
		
	
		if(tae == null)
		{
			String taeKey = options.targetAlgorithmEvaluator;
			tae = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(execConfig, taeKey,taeOptionsMap);
		} 
		
		if(tae == null)
		{
			throw new IllegalStateException("TAE should have been non-null");
		}
		//===== Note the decorators are not in general commutative
		//Specifically Run Hash codes should only see the same runs the rest of the applications see
		//Additionally retrying of crashed runs should probably happen before Abort on Crash
		
		if(options.uncleanShutdownCheck)
		{
			log.debug("[TAE] Checking for unclean shutdown");
			tae = new UncleanShutdownDetectingTargetAlgorithmEvaluator(tae);
		} else
		{
			log.debug("[TAE] Not Checking for unclean shutdown");
		}
		
		if(options.retryCount >0)
		{
			log.debug("[TAE] Automatically retrying CRASHED runs {} times " , options.retryCount);
			tae = new RetryCrashedRunsTargetAlgorithmEvaluator(options.retryCount, tae);
		}
		
		
		
		if(options.abortOnCrash)
		{
			log.debug("[TAE] Treating all crashes as aborts");
			tae = new AbortOnCrashTargetAlgorithmEvaluator(tae);
		}
		
		
		if(options.abortOnFirstRunCrash)
		{
			tae = new AbortOnFirstRunCrashTargetAlgorithmEvaluator(tae);
			
			if(options.abortOnCrash)
			{
				log.warn("[TAE] Configured to treat all crashes as aborts, it is redundant to also treat the first as an abort");
			}
		}
		
		if(options.ttaedo.transform)
		{
			log.debug("[TAE] Using Transforming Target Algorithm Evaluator");
			tae = new TransformTargetAlgorithmEvaluatorDecorator(tae, options.ttaedo);
		}
		
		if(options.verifySAT != null)
		{
			if(options.verifySAT)
			{
				log.debug("[TAE] Verifying SAT Responses");
				tae = new VerifySATTargetAlgorithmEvaluator(tae);
				
			}
		}
		
		if(options.checkSATConsistency)
		{
			log.debug("[TAE] Ensuring SAT Response consistency");
			tae = new SATConsistencyTargetAlgorithmEvaluator(tae, options.checkSATConsistencyException);
		}
		
		
		if(options.trackRunsScheduled)
		{
			String resultFile = outputDir.getAbsolutePath() + File.separator + "dispatched-runs-over-time-" + numRun + ".csv";
			log.info("[TAE] Tracking all outstanding runs to file {} ", resultFile);
			tae = new OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator(tae, resultFile, options.trackRunsScheduledResolution, "Dispatched");
			
		}
		
		
		if(!ignoreBound && options.boundRuns)
		{
			log.debug("[TAE] Bounding the number of concurrent target algorithm evaluations to {} ", options.maxConcurrentAlgoExecs);
			tae = new BoundedTargetAlgorithmEvaluator(tae, options.maxConcurrentAlgoExecs, execConfig);
			
			if(options.trackRunsScheduled)
			{
				String resultFile = outputDir.getAbsolutePath() + File.separator + "queued-runs-over-time-" + numRun + ".csv";
				log.info("[TAE] Tracking all queued runs to file {} ", resultFile);
				tae = new OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator(tae, resultFile, options.trackRunsScheduledResolution, "Queued");
			}
			
			
		}else if(ignoreBound)
		{
			log.debug("[TAE] Ignoring Bound");
		}
	

		if(options.checkResultOrderConsistent)
		{
			log.debug("[TAE] Checking that TAE honours the ordering requirement of runs");
			tae = new ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator(tae);
		}
		//==== Run Hash Code Verification should generally be one of the last
		// things we add since it is very sensitive to the actual runs being run. (i.e. a retried run or a change in the run may change a hashCode in a way the logs don't reveal
		if(hashVerifiersAllowed)
		{
			
			if(options.leakMemory)
			{
				LeakingMemoryTargetAlgorithmEvaluator.leakMemoryAmount(options.leakMemoryAmount);
				log.warn("[TAE] Target Algorithm Evaluators will leak memory. I hope you know what you are doing");
				tae = new LeakingMemoryTargetAlgorithmEvaluator(tae);
				
			}
			
			if(options.runHashCodeFile != null)
			{
				log.info("[TAE] Algorithm Execution will verify run Hash Codes");
				Queue<Integer> runHashCodes = parseRunHashCodes(options.runHashCodeFile);
				tae = new RunHashCodeVerifyingAlgorithmEvalutor(tae, runHashCodes);
				 
			} else
			{
				log.info("[TAE] Algorithm Execution will NOT verify run Hash Codes");
				tae = new RunHashCodeVerifyingAlgorithmEvalutor(tae);
			}

		}
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new TimingCheckerTargetAlgorithmEvaluator(execConfig, tae);
		
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new PrePostCommandTargetAlgorithmEvaluator(tae, options.prePostOptions);
		

		if(!options.skipOutstandingEvaluationsTAE)
		{
			//==== This class must be near the end as it is very sensitive to ordering of other TAEs, it does not change anything
			tae = new OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator(tae);
			log.debug("[TAE] Waiting / Monitoring outstanding target algorithm evaluations is supported");
		} else
		{
			log.info("[TAE] Waiting / Monitoring outstanding target algorithm evaluations will not be supported");
		}
		
		//==== Doesn't change anything and so is safe after RunHashCode
		if(options.logRequestResponses)
		{
			log.info("[TAE] Logging every request and response");
			tae = new LogEveryTargetAlgorithmEvaluatorDecorator(tae,options.logRequestResponsesRCOnly);
		}
		
		
		if(options.checkRunConfigsUnique)
		{
			log.info("[TAE] Checking that every request in a batch is unique");
			tae = new CheckForDuplicateRunConfigDecorator(tae, options.checkRunConfigsUniqueException);
		} else
		{
			log.warn("[TAE] Not Checking that every request to the TAE is unique, this may cause weird errors");
		}
		
		if(options.observeWalltimeIfNoRuntime)
		{
			log.info("[TAE] Using walltime as observer runtime if no runtime is reported, scale {} , delay {} (secs)", options.observeWalltimeScale, options.observeWalltimeDelay);
			tae = new WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(tae, options.observeWalltimeScale, options.observeWalltimeDelay);
		}
		
		if(options.killCaptimeExceedingRun)
		{
			log.debug("[TAE] Killing runs that exceed there captime by a factor of {} ", options.killCaptimeExceedingRunFactor);
			tae = new KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator(tae, options.killCaptimeExceedingRunFactor);
		}
		
		if(options.fileToWatch != null)
		{
			log.debug("[TAE] Killing runs if {} is deleted", options.fileToWatch);
			tae = new TerminateAllRunsOnFileDeleteTargetAlgorithmEvaluatorDecorator(tae, new File(options.fileToWatch));
		}
		
		if(options.synchronousObserver)
		{
			log.info("[TAE] Synchronizing notifications to the observer");
		} else
		{
			log.debug("[TAE] Skipping synchronization of observers, this may cause weird threading issues");
		}
		log.debug("Final Target Algorithm Built is {}", tae);
		return tae;
	}
	
	
	
	private static Pattern RUN_HASH_CODE_PATTERN = Pattern.compile("^Run Hash Codes:\\d+( After \\d+ runs)?\\z");
	
	private static Queue<Integer> parseRunHashCodes(File runHashCodeFile) 
	{
		log.info("Run Hash Code File Passed {}", runHashCodeFile.getAbsolutePath());
		Queue<Integer> runHashCodeQueue = new LinkedList<Integer>();
		BufferedReader bin = null;
		try {
			try{
				bin = new BufferedReader(new FileReader(runHashCodeFile));
			
				String line;
				int hashCodeCount=0;
				int lineCount = 1;
				while((line = bin.readLine()) != null)
				{
					
					Matcher m = RUN_HASH_CODE_PATTERN.matcher(line);
					if(m.find())
					{
						Object[] array = { ++hashCodeCount, lineCount, line};
						log.debug("Found Run Hash Code #{} on line #{} with contents:{}", array);
						int colonIndex = line.indexOf(":");
						int spaceIndex = line.indexOf(" ", colonIndex);
						String lineSubStr = line.substring(colonIndex+1,spaceIndex);
						runHashCodeQueue.add(Integer.valueOf(lineSubStr));
						
					} else
					{
						log.trace("No Hash Code found on line: {}", line );
					}
					lineCount++;
				}
				if(hashCodeCount == 0)
				{
					log.warn("Hash Code File Specified, but we found no hash codes");
				}
			
			} finally
			{
				if(bin != null) bin.close();
			}
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		
		return runHashCodeQueue;
		
	}

	
	
}

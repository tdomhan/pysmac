package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.OneInfinityOpenInterval;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.ReadableFileConverter;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.TAEValidator;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.ZeroInfinityOpenInterval;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceHelper;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.transform.TransformTargetAlgorithmEvaluatorDecoratorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.prepostcommand.PrePostCommandOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.validators.PositiveInteger;

@UsageTextField(title="Target Algorithm Evaluator Options", description="Options that describe and control the policy and mechanisms for algorithm execution")
public class TargetAlgorithmEvaluatorOptions extends AbstractOptions {
		
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--tae","--targetAlgorithmEvaluator"}, description="Target Algorithm Evaluator to use when making target algorithm calls", validateWith=TAEValidator.class)
	public String targetAlgorithmEvaluator = "CLI";
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--abort-on-crash","--abortOnCrash"}, description="treat algorithm crashes as an ABORT (Useful if algorithm should never CRASH). NOTE:  This only aborts if all retries fail.")
	public boolean abortOnCrash = false;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--abort-on-first-run-crash","--abortOnFirstRunCrash"}, description="if the first run of the algorithm CRASHED treat it as an ABORT, otherwise allow crashes.")
	public boolean abortOnFirstRunCrash = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--retry-crashed-count","--retryCrashedRunCount","--retryTargetAlgorithmRunCount"}, description="number of times to retry an algorithm run before reporting crashed (NOTE: The original crashes DO NOT count towards any time limits, they are in effect lost). Additionally this only retries CRASHED runs, not ABORT runs, this is by design as ABORT is only for cases when we shouldn't bother further runs", validateWith=NonNegativeInteger.class)
	public int retryCount = 0;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--bound-runs","--boundRuns"}, description="[DEPRECATED] (Use the option on the TAE instead if available) if true, permit only --cores number of runs to be evaluated concurrently. ")
	public boolean boundRuns = false;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cores","--numConcurrentAlgoExecs","--maxConcurrentAlgoExecs","--numberOfConcurrentAlgoExecs"}, description=" [DEPRECATED] (Use the TAE option instead if available) maximum number of concurrent target algorithm executions", validateWith=PositiveInteger.class)
	public int maxConcurrentAlgoExecs = 1;
	
	@UsageTextField(defaultValues="", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--run-hashcode-file","--runHashCodeFile"}, description="file containing a list of run hashes one per line: Each line should be: \"Run Hash Codes: (Hash Code) After (n) runs\". The number of runs in this file need not match the number of runs that we execute, this file only ensures that the sequences never diverge. Note the n is completely ignored so the order they are specified in is the order we expect the hash codes in this version. Finally note you can simply point this at a previous log and other lines will be disregarded", converter=ReadableFileConverter.class)
	public File runHashCodeFile;

	@UsageTextField( level=OptionLevel.DEVELOPER)
	@Parameter(names={"--leak-memory","--leakMemory"}, hidden=true, description="leaks some amount of memory for every run")
	public boolean leakMemory = false;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--leak-memory-amount","--leakMemoryAmount"}, hidden=true, description="amount of memory in bytes to leak")
	public int leakMemoryAmount = 1024;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--verify-sat","--verify-SAT","--verifySAT"}, description="Check SAT/UNSAT/UNKNOWN responses against Instance specific information (if null then performs check if every instance has specific information in the following domain {SAT, UNSAT, UNKNOWN, SATISFIABLE, UNSATISFIABLE}")
	public Boolean verifySAT = null;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--check-sat-consistency","--checkSATConsistency"}, description="Ensure that runs on the same problem instance always return the same SAT/UNSAT result")
	public boolean checkSATConsistency = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--check-sat-consistency-exception","--checkSATConsistencyException"}, description="Throw an exception if runs on the same problem instance disagree with respect to SAT/UNSAT")
	public boolean checkSATConsistencyException = true;
	
	@ParametersDelegate
	public PrePostCommandOptions prePostOptions = new PrePostCommandOptions();

	@UsageTextField( level=OptionLevel.DEVELOPER)
	@Parameter(names={"--check-result-order-consistent","--checkResultOrderConsistent"}, description="Check that the TAE is returning responses in the correct order")
	public boolean checkResultOrderConsistent;
	
	@UsageTextField( level=OptionLevel.DEVELOPER)
	@Parameter(names="--skip-outstanding-eval-tae", description="If set to true code, the TAE will not be wrapped by a decorator to support waiting for outstanding runs")
	public boolean skipOutstandingEvaluationsTAE = false;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--log-requests-responses", description="If set to true all evaluation requests will be logged as they are submitted and completed")
	public boolean logRequestResponses = false; 
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--log-requests-responses-rc-only","--log-requests-responses-rc"}, description="If set to true we will only log the run configuration when a run completes ")
	public boolean logRequestResponsesRCOnly = false;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--track-scheduled-runs", description="If true outputs a file in the output directory that outlines how many runs were being evaluated at any given time")
	public boolean trackRunsScheduled; 

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--track-scheduled-runs-resolution", description="We will bucket changes into this size", validateWith=ZeroInfinityOpenInterval.class)
	public double trackRunsScheduledResolution = 1; 
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--check-for-unclean-shutdown", description="If true, we will try and detect an unclean shutdown of the Target Algorithm Evaluator")
	public boolean uncleanShutdownCheck = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--kill-run-exceeding-captime", description="Attempt to kill runs that exceed their captime by some amount")
	public boolean killCaptimeExceedingRun = true;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--kill-run-exceeding-captime-factor", description="Attempt to kill the run that exceed their captime by this factor", validateWith=OneInfinityOpenInterval.class)
	public double killCaptimeExceedingRunFactor = 10.0;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--check-for-unique-runconfigs", description="Checks that all submitted Run Configs in a batch are unique")
	public boolean checkRunConfigsUnique = true;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--check-for-unique-runconfigs-exception", description="If true, we will throw an exception if duplicate run configurations are detected")
	public boolean checkRunConfigsUniqueException = true;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--observer-walltime-if-no-runtime", description="If true and the target algorithm doesn't update us with runtime information we report wallclock time")
	public boolean observeWalltimeIfNoRuntime = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--observer-walltime-scale", description="What factor of the walltime should we use as the runtime (generally recommended is the 0.95 times the number of cores)", validateWith=ZeroInfinityOpenInterval.class)
	public double observeWalltimeScale = 0.95;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--observer-walltime-delay", description="How long to wait for an update with runtime information, before we use the walltime. With the 5 seconds and an scale of 0.95, it means we will see 0,0,0,0...,4.95...", validateWith=ZeroInfinityOpenInterval.class)
	public double observeWalltimeDelay=5.0;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--synchronize-observers", description="Synchronize calls to the observer (this helps simplify memory visibility issues)", hidden=true)
	public boolean synchronousObserver = true;

	@UsageTextField(defaultValues="~/.aclib/tae.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--tae-default-file"}, description="file that contains default settings for Target Algorithm Evaluators")
	@ParameterFile(ignoreFileNotExists = true) 
	public File taeDefaults = HomeFileUtils.getHomeFile(".aclib" + File.separator  + "tae.opt");
	
	@ParametersDelegate
	public TransformTargetAlgorithmEvaluatorDecoratorOptions ttaedo = new TransformTargetAlgorithmEvaluatorDecoratorOptions();
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--kill-runs-on-file-delete"}, description="All runs will be forcibly killed if the file is deleted. This option may cause the application to enter an infinite loop if the file is deleted, so care is needed. As a rule, you need to set this and some other option to point to the same file, if there is another option, then the application will probably shutdown nicely, if not, then it will probably infinite loop." )
	public String fileToWatch = null;
	
	
	/**
	 * Checks if the problem instances are compatible with the verify sat option
	 * @param instances 
	 */
	public void checkProblemInstancesCompatibleWithVerifySAT(List<ProblemInstance> instances) {

		Logger log = LoggerFactory.getLogger(getClass());
		if(verifySAT == null)
		{
			boolean verifySATCompatible = ProblemInstanceHelper.isVerifySATCompatible(instances);
			if(verifySATCompatible)
			{
				log.debug("Instance Specific Information is compatible with Verifying SAT, enabling option");
				verifySAT = true;
			} else
			{
				log.debug("Instance Specific Information is NOT compatible with Verifying SAT, disabling option");
				verifySAT = false;
			}
			
		} else if(verifySAT == true)
		{
			boolean verifySATCompatible = ProblemInstanceHelper.isVerifySATCompatible(instances);
			if(!verifySATCompatible)
			{
				log.warn("Verify SAT set to true, but some instances have instance specific information that isn't in {SAT, SATISFIABLE, UNKNOWN, UNSAT, UNSATISFIABLE}");
			}
				
		}
		
	}
	
	/**
	 * Retrieves the available list of target algorithm evaluators
	 * @return 
	 */
	public Map<String, AbstractOptions> getAvailableTargetAlgorithmEvaluators()
	{
		return TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	}
	/**
	 * Retrieves a target algorithm evaluator
	 * 
	 * @param execConfig	execution configuration object for the target algorithm
	 * @param taeOptionsMap	options for all taes
	 * @param outputDir		output directory
	 * @param numRun		number of our run (used for TAEs that output files as a suffix generally)
	 * @return
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig,  Map<String, AbstractOptions> taeOptionsMap, String outputDir, int numRun)
	{
		return TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(this, execConfig, true, false, taeOptionsMap, null, new File(outputDir), numRun);
	}
	
	
	/**
	 * Retrieves a target algorithm evaluator
	 * 
	 * @param execConfig			execution configuration for the target algorithm
	 * @param hashVerifiersAllowed	whether hash verifies should be applied
	 * @param ignoreBound			whether we should ignore the bound argument
	 * @param taeOptionsMap			options for all avaliable TAEs
	 * @param outputDir				output directory
	 * @param numRun				number of our run (used for TAEs that output files as a suffix generally)
	 * @return
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, String outputDir, int numRun)
	{
		return TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(this, execConfig, hashVerifiersAllowed, ignoreBound, taeOptionsMap, null,new File(outputDir), numRun);
	}
	
	/**
	 * Retrieves a target algorithm evaluator
	 * 
	 * @param execConfig	execution configuration object for the target algorithm
	 * @param taeOptionsMap	options for all available TAEs
	 * @param outputDir		output directory
	 * @param numRun		number of our run (used for TAEs that output files as a suffix generally)
	 * @return
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig,  Map<String, AbstractOptions> taeOptionsMap, File outputDir, int numRun)
	{
		return TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(this, execConfig, true, false, taeOptionsMap, null, outputDir, numRun);
	}
	
	
	/**
	 * Retrieves a target algorithm evaluator
	 * 
	 * @param execConfig			execution configuration for the target algorithm
	 * @param hashVerifiersAllowed	whether hash verifies should be applied
	 * @param ignoreBound			whether we should ignore the bound argument
	 * @param taeOptionsMap			options for all available TAEs
	 * @param outputDir				output directory
	 * @param numRun				number of our run (used for TAEs that output files as a suffix generally)
	 * @return
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, File outputDir, int numRun)
	{
		return TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(this, execConfig, hashVerifiersAllowed, ignoreBound, taeOptionsMap, null, outputDir, numRun);
	}

	/**
	 * Retrieves a target algorithm evaluator
	 * @param execConfig	execution configuration for the target algorithm
	 * @param taeOptions	options for all available TAEs
	 * @return
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig,Map<String, AbstractOptions> taeOptions) 
	{
		return getTargetAlgorithmEvaluator(execConfig, taeOptions, new File(".").getAbsolutePath(), 0);
	}

	/**
	 * Turns all options that would probably cause this application to crash off.
	 */
	public void turnOffCrashes() {
		Logger log = LoggerFactory.getLogger(getClass());
		log.info("Abort on Crash,Abort on First Run Crash, Verify SAT, and use Walltime if no Runtime are DISABLED as these options may cause unwanted crashes");
		abortOnCrash = false;
		abortOnFirstRunCrash = false;
		verifySAT = false;
		checkSATConsistency = true;
		checkSATConsistencyException = false;
		this.observeWalltimeIfNoRuntime = false;
		this.runHashCodeFile = null;
	}
	
	
}

package ca.ubc.cs.beta.aclib.smac;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ca.ubc.cs.beta.aclib.acquisitionfunctions.AcquisitionFunctions;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.configspace.tracking.ParamConfigurationOriginTrackingOptions;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.help.HelpOptions;
import ca.ubc.cs.beta.aclib.initialization.InitializationMode;
import ca.ubc.cs.beta.aclib.initialization.classic.ClassicInitializationProcedureOptions;
import ca.ubc.cs.beta.aclib.initialization.doublingcapping.DoublingCappingInitializationProcedureOptions;
import ca.ubc.cs.beta.aclib.initialization.table.UnbiasChallengerInitializationProcedure;
import ca.ubc.cs.beta.aclib.initialization.table.UnbiasChallengerInitializationProcedureOptions;
import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.*;
import ca.ubc.cs.beta.aclib.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.model.ModelBuildingOptions;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.RandomForestOptions;
import ca.ubc.cs.beta.aclib.options.RunGroupOptions;
import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aclib.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceOptions.TrainTestInstances;
import ca.ubc.cs.beta.aclib.random.SeedOptions;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPoolConstants;
import ca.ubc.cs.beta.aclib.state.StateFactory;
import ca.ubc.cs.beta.aclib.state.StateFactoryOptions;
import ca.ubc.cs.beta.aclib.state.WarmStartOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;


/**
 * Represents the configuration for SMAC, 
 * 
 * @author seramage
 *
 *
 *
 */
@UsageTextField(title="SMAC Options", description="General Options for Running SMAC", claimRequired={"--instanceFile"}, noarg=SMACNoArgHandler.class)
public class SMACOptions extends AbstractOptions {
	
	@UsageTextField(defaultValues="Defaults to true when --runObj is RUNTIME, false otherwise", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--adaptive-capping","--ac","--adaptiveCapping"}, description="Use Adaptive Capping")
	public Boolean adaptiveCapping = null;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--always-run-initial-config","--alwaysRunInitialConfiguration"}, description="if true we will always run the default and switch back to it if it is better than the incumbent")
	public boolean alwaysRunInitialConfiguration = false;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--ac-add-slack","--capAddSlack"}, description="amount to increase computed adaptive capping value of challengers by (post scaling)", validateWith=ZeroInfinityOpenInterval.class)
	public double capAddSlack = 1;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--ac-mult-slack","--capSlack"}, description="amount to scale computed adaptive capping value of challengers by", validateWith=ZeroInfinityOpenInterval.class)
	public double capSlack = 1.3;

	
	@ParametersDelegate
	public ClassicInitializationProcedureOptions classicInitModeOpts = new ClassicInitializationProcedureOptions();
	
	@ParametersDelegate
	public DoublingCappingInitializationProcedureOptions dciModeOpts = new DoublingCappingInitializationProcedureOptions();
	
	@ParametersDelegate
	public UnbiasChallengerInitializationProcedureOptions ucip = new UnbiasChallengerInitializationProcedureOptions();
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--deterministic-instance-ordering","--deterministicInstanceOrdering"}, description="If true, instances will be selected from the instance list file in the specified order")
	public boolean deterministicInstanceOrdering = false;
	
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.BASIC)
	@Parameter(names={"--validation","--doValidation"}, description="perform validation when SMAC completes")
	public boolean doValidation = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--exec-mode","--execution-mode","--executionMode"}, description="execution mode of the automatic configurator")
	public ExecutionMode execMode = ExecutionMode.SMAC;

	@CommandLineOnly
	@UsageTextField(defaultValues="<current working directory>", level=OptionLevel.BASIC)
	@Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="root directory for experiments Folder")
	public String experimentDir = System.getProperty("user.dir") + File.separator + "";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--acq-func","--acquisition-function", "--ei-func","--expected-improvement-function","--expectedImprovementFunction"}, description="acquisition function to use during local search")
	public AcquisitionFunctions expFunc = null;
	
	@ParametersDelegate
	public HelpOptions help = new HelpOptions();
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--initial-challenger-runs","--initialN","--initialChallenge"}, description="initial amount of runs to request when intensifying on a challenger", validateWith=FixedPositiveInteger.class)
	public int initialChallengeRuns = 1;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--initial-incumbent","--initialIncumbent"}, description="Initial Incumbent to use for configuration (you can use RANDOM, or DEFAULT as a special string to get a RANDOM or the DEFAULT configuration as needed). Other configurations are specified as: -name 'value' -name 'value' ... For instance: --quick-sort 'on' ")
	public String initialIncumbent = "DEFAULT";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--init-mode","--initialization-mode","--initMode","--initializationMode"}, description="Initialization Mode")
	public InitializationMode initializationMode = InitializationMode.CLASSIC;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--intensification-percentage","--intensificationPercentage","--frac_rawruntime"}, description="percent of time to spend intensifying versus model learning", validateWith=ZeroOneHalfOpenRightDouble.class)
	public double intensificationPercentage = 0.50;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--iterativeCappingBreakOnFirstCompletion"}, description="In Phase 2 of the initialization phase, we will abort the first time something completes and not look at anything else with the same kappa limits")
	public boolean iterativeCappingBreakOnFirstCompletion = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--iterativeCappingK"}, description="Iterative Capping K")
	public int iterativeCappingK = 1;
	
	@ParametersDelegate
	public ComplexLoggingOptions logOptions = new ComplexLoggingOptions();
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--max-incumbent-runs","--maxIncumbentRuns","--maxRunsForIncumbent"}, description="maximum number of incumbent runs allowed", validateWith=FixedPositiveInteger.class)
	public int maxIncumbentRuns = 2000;
	
	@ParametersDelegate
	public ModelBuildingOptions mbOptions = new ModelBuildingOptions();
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--model-hashcode-file","--modelHashCodeFile"}, description="file containing a list of model hashes one per line with the following text per line: \"Preprocessed Forest Built With Hash Code: (n)\" or \"Random Forest Built with Hash Code: (n)\" where (n) is the hashcode", converter=ReadableFileConverter.class, hidden = true)
	public File modelHashCodeFile;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--num-challengers","--numChallengers","--numberOfChallengers"}, description="number of challengers needed for local search", validateWith=FixedPositiveInteger.class)
	public int numberOfChallengers = 10;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--num-ei-random","--numEIRandomConfigs","--numberOfRandomConfigsInEI","--numRandomConfigsInEI","--numberOfEIRandomConfigs"} , description="number of random configurations to evaluate during EI search", validateWith=NonNegativeInteger.class)
	public int numberOfRandomConfigsInEI = 10000;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--num-pca","--numPCA"}, description="number of principal components features to use when building the model", validateWith=FixedPositiveInteger.class)
	public int numPCA = 7;

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@ParameterFile
	@Parameter(names={"--option-file","--optionFile"}, description="read options from file")
	public File optionFile;

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@ParameterFile
	@Parameter(names={"--option-file2","--optionFile2","--secondaryOptionsFile"}, description="read options from file")
	public File optionFile2;

	@ParametersDelegate
	public RandomForestOptions randomForestOptions = new RandomForestOptions();


	@ParametersDelegate
	public RunGroupOptions runGroupOptions = new RunGroupOptions("%SCENARIO_NAME-%executionMode-ac-%adaptiveCapping-cores%cores-cutoff%cutoffTime-%DATE"); 

	@ParametersDelegate
	public ScenarioOptions scenarioConfig = new ScenarioOptions();

	
	@ParametersDelegate
	public SeedOptions seedOptions = new SeedOptions();
	
	@UsageTextField(defaultValues="~/.aclib/smac.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--smac-default-file","--smacDefaultsFile"}, description="file that contains default settings for SMAC")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacDefaults = HomeFileUtils.getHomeFile(".aclib" + File.separator  + "smac.opt");
	
	@ParametersDelegate
	public StateFactoryOptions stateOpts = new StateFactoryOptions();

	@ParametersDelegate
	public ParamConfigurationOriginTrackingOptions trackingOptions= new ParamConfigurationOriginTrackingOptions();

	@ParametersDelegate
	public ValidationOptions validationOptions = new ValidationOptions();
	
	@ParametersDelegate
	public WarmStartOptions warmStartOptions = new WarmStartOptions();
	
	
	@UsageTextField(defaultValues="0 which should cause it to run exactly the same as the stand-alone utility.")
	@Parameter(names="--validation-seed", description="Seed to use for validating SMAC")
	public int validationSeed = 0;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--save-runs-every-iteration"}, description="if true will save the runs and results file to disk every iteration. Useful if your runs are expensive and your cluster unreliable, not recommended if your runs are short as this may add an unacceptable amount of overhead")
	public boolean saveRunsEveryIteration = false;
	
	
	/**
	 * Checks if the verify sat option is compatible with this set of probelm instances
	 * @param instances 	The problem instances
	 */
	public void checkProblemInstancesCompatibleWithVerifySAT(List<ProblemInstance> instances)
	{
		this.scenarioConfig.algoExecOptions.taeOpts.checkProblemInstancesCompatibleWithVerifySAT(instances);
	}
	
	public AlgorithmExecutionConfig getAlgorithmExecutionConfig() {
		return this.scenarioConfig.getAlgorithmExecutionConfig(experimentDir);
	}
	
	public String getOutputDirectory(String runGroupName)
	{
		File outputDir = new File(this.scenarioConfig.outputDirectory + File.separator + runGroupName);
		if(!outputDir.isAbsolute())
		{
			outputDir = new File(experimentDir + File.separator + this.scenarioConfig.outputDirectory + File.separator + runGroupName);
		}
		
		return outputDir.getAbsolutePath();
	}

	/**
	 * Returns a state factory
	 * @param outputDir	output directory
	 * @return
	 */
	public StateFactory getRestoreStateFactory(String outputDir) {
		return stateOpts.getRestoreStateFactory(outputDir, this.seedOptions.numRun);
	}

	public String getRunGroupName(Collection<AbstractOptions> opts)
	{	
		opts = new HashSet<AbstractOptions>(opts);
		opts.add(this);
		return runGroupOptions.getRunGroupName(opts);	
	}
	
	public StateFactory getSaveStateFactory(String outputDir) {
		return stateOpts.getSaveStateFactory(outputDir, this.seedOptions.numRun);
	}

	/**
	 * Gets both the training and the test problem instances
	 * 
	 * @param experimentDirectory	Directory to search for instance files
	 * @param trainingSeed			Seed to use for the training instances
	 * @param testingSeed			Seed to use for the testing instances
	 * @param trainingRequired		Whether the training instance file is required
	 * @param testRequired			Whether the test instance file is required
	 * @return
	 * @throws IOException
	 */
	public TrainTestInstances getTrainingAndTestProblemInstances(SeedableRandomPool instancePool, SeedableRandomPool testInstancePool) throws IOException
	{
			return this.scenarioConfig.getTrainingAndTestProblemInstances(this.experimentDir, instancePool.getRandom(SeedableRandomPoolConstants.INSTANCE_SEEDS).nextInt(), testInstancePool.getRandom(SeedableRandomPoolConstants.TEST_SEED_INSTANCES).nextInt(), true, this.doValidation, false, false);
	}

	public void saveContextWithState(ParamConfigurationSpace configSpace, InstanceListWithSeeds trainingILWS,	StateFactory sf)
	{
		this.stateOpts.saveContextWithState(configSpace, trainingILWS, this.scenarioConfig.scenarioFile, sf);
	}
}

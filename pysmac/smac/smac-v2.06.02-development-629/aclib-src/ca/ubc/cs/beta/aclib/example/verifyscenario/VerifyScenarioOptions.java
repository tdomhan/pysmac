package ca.ubc.cs.beta.aclib.example.verifyscenario;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.aclib.help.HelpOptions;
import ca.ubc.cs.beta.aclib.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aclib.logging.LoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="Verify Scenario Utility", description="Verifies scenario files are intact and complete", noarg=VerifyScenarioNoArgumentHandler.class)
public class VerifyScenarioOptions extends AbstractOptions{

	@Parameter(names={"--scenario-files","--scenario-file","--scenarios"}, description="Scenario Files to validate", variableArity=true)
	public List<String> scenarios = new ArrayList<String>();
	
	@ParametersDelegate
	public HelpOptions helpOptions = new HelpOptions();
	
	@ParametersDelegate
	public LoggingOptions logOptions = new ConsoleOnlyLoggingOptions();

	@CommandLineOnly
	@UsageTextField(defaultValues="<current working directory>", level=OptionLevel.BASIC)
	@Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="root directory for experiments Folder")
	public String experimentDir = System.getProperty("user.dir") + File.separator + "";

	@Parameter(names="--verify-instances", description="Verify every instance exists on disk")
	public boolean checkInstances = true;

	
	@Parameter(names="--output-details", description="Output details of the scenario")
	public boolean details = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--restore-args","--restore-scenario-arguments"}, description="A string that will be used to restore the individual state files, this allows you to override options in the saved scenarios for instance \"--cutoffTime 5.0\" would change the cutoff time on all restored scenarios")
	public String restoreScenarioArguments = "";
	
}

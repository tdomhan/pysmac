package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli;

import java.io.File;

import ca.ubc.cs.beta.aclib.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;


@UsageTextField(title="Command Line Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator executes commands via the command line and the standard wrapper interface. ")
public class CommandLineTargetAlgorithmEvaluatorOptions extends AbstractOptions {
	

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--cli-observer-frequency", description="How often to notify observer of updates (in milli-seconds)", validateWith=FixedPositiveInteger.class)
	public int observerFrequency = 750;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--cli-concurrent-execution", description="Whether to allow concurrent execution ")
	public boolean concurrentExecution = true;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--cli-cores", description="Number of cores to use to execute runs. In other words the number of requests to run at a given time.", validateWith=FixedPositiveInteger.class)
	public int cores = 1;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-call-strings","--log-all-call-strings","--logAllCallStrings"}, description="log every call string")
	public boolean logAllCallStrings = false;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-process-output","--log-all-process-output","--logAllProcessOutput"}, description="log all process output")
	public boolean logAllProcessOutput = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-listen-for-updates"}, description="If true will create a socket and set environment variables so that we can have updates of CPU time")
	public boolean listenForUpdates = true;
	
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-pg-nice-kill-cmd"}, description="Command to execute to try and ask the process group to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String pgNiceKillCommand = "bash -c \"kill -s TERM -%pid\"";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-pg-force-kill-cmd"}, description="Command to execute to try and ask the process group to terminate nicely (generally a SIGKILL in Unix). Note %pid will be replaced with the PID we determine.")
	public String pgForceKillCommand = "bash -c \"kill -s KILL -%pid\"";
	
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-proc-nice-kill-cmd"}, description="Command to execute to try and ask the process to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String procNiceKillCommand = "kill -s TERM %pid";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-proc-force-kill-cmd"}, description="Command to execute to try and ask the process to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String procForceKillCommand = "kill -s KILL %pid";
	
	
	@UsageTextField(defaultValues="~/.aclib/cli-tae.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-default-file"}, description="file that contains default settings for CLI Target Algorithm Evaluator (it is recommended that you use this file to set the kill commands)")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacDefaults = HomeFileUtils.getHomeFile(".aclib" + File.separator  + "cli-tae.opt");
	
	
	
}

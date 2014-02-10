package ca.ubc.cs.beta.aclib.execconfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpaceOptions;
import ca.ubc.cs.beta.aclib.misc.jcommander.converter.BinaryDigitBooleanConverter;
import ca.ubc.cs.beta.aclib.misc.jcommander.converter.StringToDoubleConverterWithMax;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.ZeroInfinityOpenInterval;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.Semantics;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;


/**
 * Options object that defines arguments for Target Algorithm Execution
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */

@UsageTextField(title="Algorithm Execution Options", description="Options related to invoking the target algorithm")
public class AlgorithmExecutionOptions extends AbstractOptions {
	
	@Parameter(names={"--algo-exec","--algoExec", "--algo"}, description="command string to execute algorithm with", required=true)
	public String algoExec;
	
	@Parameter(names={"--algo-exec-dir","--exec-dir","--execDir","--execdir"}, description="working directory to execute algorithm in", required=true)
	public String algoExecDir;
	
	@Parameter(names={"--algo-deterministic","--deterministic"}, description="treat the target algorithm as deterministic", converter=BinaryDigitBooleanConverter.class)
	public boolean deterministic;

	@Semantics(name="MAX_SUBRUN_CPUTIME", domain="OPT")
	@Parameter(names={"--algo-cutoff-time","--cutoff-time","--cutoffTime","--cutoff_time"}, description="CPU time limit for an individual target algorithm run", required=true, validateWith=ZeroInfinityOpenInterval.class)
	public double cutoffTime;
	
	@Semantics(name="MAX_SUBRUN_RUNLENGTH", domain="OPT")
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--algo-cutoff-length","--cutoffLength","--cutoff_length"}, description="cap limit for an individual run [not implemented currently]", converter=StringToDoubleConverterWithMax.class, hidden=true)
	public double cutoffLength = -1.0;
	
	@ParametersDelegate
	public TargetAlgorithmEvaluatorOptions taeOpts = new TargetAlgorithmEvaluatorOptions();
	
	@ParametersDelegate
	public ParamConfigurationSpaceOptions paramFileDelegate = new ParamConfigurationSpaceOptions();
	
	
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfig getAlgorithmExecutionConfig()
	{
		return getAlgorithmExecutionConfig(null);
	}
	
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfig getAlgorithmExecutionConfigSkipDirCheck()
	{
		return getAlgorithmExecutionConfig(Collections.<String> emptyList(), false);
	}
	
	
	public AlgorithmExecutionConfig getAlgorithmExecutionConfig(String experimentDir)
	{
		return getAlgorithmExecutionConfig(experimentDir, true);
	}
	
	public AlgorithmExecutionConfig getAlgorithmExecutionConfig(String experimentDir, boolean checkExecDir)
	{
		if(experimentDir == null)
		{
			return getAlgorithmExecutionConfig(Collections.<String> emptyList(), checkExecDir);
		} else
		{
			return getAlgorithmExecutionConfig(Collections.singletonList(experimentDir), checkExecDir);
		}
		
	}
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @param inputDirs the experiment directory to search for parameter configurations (it is expected that the first one will be the experiment directory)
	 * @param checkExecDir  if <code>true</code> we will check that the execution directory exists
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfig getAlgorithmExecutionConfig(List<String> inputDirs, boolean checkExecDir)
	{
		List<String> dirToSearch = new ArrayList<String>();
		if(inputDirs != null)
		{
			dirToSearch.addAll(inputDirs);
		}
		

		File execDir = new File(algoExecDir);
		if(checkExecDir)
		{
			if(!execDir.exists())
			{
				if (inputDirs.size()> 0)
				{
					//Only check if there is another place to look
					execDir = new File(inputDirs.get(0) + File.separator + algoExecDir);
				}
				
				if(!execDir.exists())
				{
					throw new ParameterException("Cannot find execution algorithm execution directory: " + algoExecDir +  "  in context:" + dirToSearch);
				}
			}
			
			dirToSearch.add(execDir.getAbsolutePath());
		}
		
		return new AlgorithmExecutionConfig(algoExec, execDir.getAbsolutePath(), paramFileDelegate.getParamConfigurationSpace(dirToSearch), false, deterministic, this.cutoffTime);
	}
}

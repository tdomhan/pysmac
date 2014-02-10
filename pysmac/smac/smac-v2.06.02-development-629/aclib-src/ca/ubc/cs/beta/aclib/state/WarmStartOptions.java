package ca.ubc.cs.beta.aclib.state;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.runhistory.NewRunHistory;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
import ca.ubc.cs.beta.aclib.state.converter.AutoAsMaxConverter;
import ca.ubc.cs.beta.aclib.state.legacy.LegacyStateFactory;
import ca.ubc.cs.beta.aclib.state.nullFactory.NullStateFactory;

import com.beust.jcommander.Parameter;

@UsageTextField(hiddenSection=true)
public class WarmStartOptions extends AbstractOptions {
	
	@CommandLineOnly
	@UsageTextField(defaultValues="N/A (No state is being warmstarted)", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--warmstart", "--warmstart-from"}, description="location of state to use for warm-starting")
	public String warmStartStateFrom = null;
	
	@CommandLineOnly
	@UsageTextField(defaultValues="AUTO (if being restored)", level=OptionLevel.ADVANCED)
	@Parameter(names={"--warmstart-iteration"}, description="iteration of the state to use for warm-starting, use \"AUTO\" to automatically pick the last iteration", converter=AutoAsMaxConverter.class)
	public Integer restoreIteration = Integer.MAX_VALUE;
	
	public void getWarmStartState(ParamConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfig execConfig, RunHistory rhToPopulate)
	{
		
		Logger log = LoggerFactory.getLogger(getClass());
		
		if(warmStartStateFrom != null)
		{
			log.info("Warm-starting from folder {} " ,warmStartStateFrom);
			StateFactory sf = new LegacyStateFactory(null, warmStartStateFrom);
			sf.getStateDeserializer("it", restoreIteration, configSpace, instances, execConfig, rhToPopulate);
		}
		
	}

	

}

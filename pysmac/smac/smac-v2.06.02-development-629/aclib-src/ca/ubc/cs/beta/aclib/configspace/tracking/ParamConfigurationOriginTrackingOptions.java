package ca.ubc.cs.beta.aclib.configspace.tracking;

import java.io.File;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.AutomaticConfigurationEnd;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aclib.eventsystem.handlers.ParamConfigurationIncumbentChangerOriginTracker;
import ca.ubc.cs.beta.aclib.eventsystem.handlers.ParamConfigurationOriginLogger;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;


@UsageTextField(hiddenSection=true)
public class ParamConfigurationOriginTrackingOptions extends AbstractOptions {

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@Parameter(names={"--config-tracking"}, description="Take measurements of configuration as it goes through it's lifecycle and write to file (in state folder)")
	public boolean configTracking;
	
	
	public ParamConfigurationOriginTracker getTracker(EventManager eventManager, ParamConfiguration initialIncumbent, String outputDir, ThreadSafeRunHistory rh, AlgorithmExecutionConfig execConfig, int numRun)
	{
		if(configTracking)
		{
			ParamConfigurationOriginTracker configTracker = new RealParamConfigurationOriginTracker();
			configTracker.addConfiguration(initialIncumbent, "DEFAULT", "true");
			eventManager.registerHandler(AutomaticConfigurationEnd.class, new ParamConfigurationOriginLogger(configTracker, outputDir + File.separator + "state-run" + numRun + File.separator , rh, System.currentTimeMillis(), execConfig.getAlgorithmCutoffTime()));
			eventManager.registerHandler(IncumbentPerformanceChangeEvent.class, new ParamConfigurationIncumbentChangerOriginTracker(configTracker, rh, execConfig.getAlgorithmCutoffTime()));
			return configTracker;
		} else
		{
			return new NullParamConfigurationOriginTracker();
		}
	}
}

package ca.ubc.cs.beta.aclib.termination;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.FixedPositiveLong;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.Semantics;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.termination.standard.AlgorithmRunLimitCondition;
import ca.ubc.cs.beta.aclib.termination.standard.CPULimitCondition;
import ca.ubc.cs.beta.aclib.termination.standard.FileDeletedTerminateCondition;
import ca.ubc.cs.beta.aclib.termination.standard.ModelIterationTerminationCondition;
import ca.ubc.cs.beta.aclib.termination.standard.NoRunsForManyChallengesEvent;
import ca.ubc.cs.beta.aclib.termination.standard.WallClockLimitCondition;

@UsageTextField(hiddenSection=false, description="Options that control how long the scenario will run for", title="Scenario Configuration Limit Options")
public class TerminationCriteriaOptions extends AbstractOptions {

	@Semantics(name="MAX_CPUTIME", domain="SCENARIO")
	@Parameter(names={"--tunertime-limit","--tuner-timeout","--tunerTimeout"}, description="limits the total cpu time allowed between SMAC and the target algorithm runs during the automatic configuration phase", validateWith=NonNegativeInteger.class)
	public int tunerTimeout = Integer.MAX_VALUE;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--iteration-limit","--numIterations","--numberOfIterations"}, description = "limits the number of iterations allowed during automatic configuration phase", validateWith=FixedPositiveInteger.class)
	public int numIterations = Integer.MAX_VALUE;
	
	@Parameter(names={"--wallclock-limit","--runtime-limit","--runtimeLimit", "--wallClockLimit"}, description = "limits the total wall-clock time allowed during the automatic configuration phase", validateWith=FixedPositiveInteger.class)
	public int runtimeLimit = Integer.MAX_VALUE;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--runcount-limit","--totalNumRunsLimit","--numRunsLimit","--numberOfRunsLimit"} , description = "limits the total number of target algorithm runs allowed during the automatic configuration phase ", validateWith=FixedPositiveLong.class)
	public long totalNumRunsLimit = Long.MAX_VALUE;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--use-cpu-time-in-tunertime","--countSMACTimeAsTunerTime"}, description="include the CPU Time of SMAC as part of the tunerTimeout")
	public boolean countSMACTimeAsTunerTime = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--max-norun-challenge-limit","--maxConsecutiveFailedChallengeIncumbent"}, description="if the parameter space is too small we may get to a point where we can make no new runs, detecting this condition is prohibitively expensive, and this heuristic controls the number of times we need to try a challenger and get no new runs before we give up")
	public int challengeIncumbentAttempts = 1000;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--terminate-on-delete"}, description="Terminate the procedure if this file is deleted")
	public String fileToWatch = null;
	
	
	public CompositeTerminationCondition getTerminationConditions()
	{
		List<TerminationCondition> termConds = new ArrayList<TerminationCondition>();
		
		termConds.add(new CPULimitCondition(tunerTimeout, countSMACTimeAsTunerTime));
		termConds.add(new WallClockLimitCondition(System.currentTimeMillis(),runtimeLimit));
		termConds.add(new AlgorithmRunLimitCondition(totalNumRunsLimit));
		termConds.add(new ModelIterationTerminationCondition(this.numIterations));
		termConds.add(new NoRunsForManyChallengesEvent(challengeIncumbentAttempts));
		if(fileToWatch != null)
		{
			termConds.add(new FileDeletedTerminateCondition(new File(fileToWatch)));
		}
		return new CompositeTerminationCondition(termConds);
	}
	
	
}

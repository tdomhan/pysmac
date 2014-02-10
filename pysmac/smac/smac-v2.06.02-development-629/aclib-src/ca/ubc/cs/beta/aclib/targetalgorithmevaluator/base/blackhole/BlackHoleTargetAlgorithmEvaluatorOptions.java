package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.blackhole;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

@UsageTextField(title="Blackhole Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator simply never returns any runs", level=OptionLevel.DEVELOPER)
public class BlackHoleTargetAlgorithmEvaluatorOptions extends AbstractOptions {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--blackhole-warnings", description="Suppress warning that is generated")
	public boolean warnings = true;
	
}

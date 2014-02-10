package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli;

import java.util.concurrent.ArrayBlockingQueue;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;
@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class CommandLineTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  {

	
	@Override
	public String getName() {
		return "CLI";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig config, AbstractOptions options) {

		CommandLineTargetAlgorithmEvaluatorOptions cliOpts = (CommandLineTargetAlgorithmEvaluatorOptions) options;

		
		return new CommandLineTargetAlgorithmEvaluator(config, cliOpts );
	}

	@Override
	public CommandLineTargetAlgorithmEvaluatorOptions getOptionObject()
	{
		return new CommandLineTargetAlgorithmEvaluatorOptions();
	}

	
	public static CommandLineTargetAlgorithmEvaluatorOptions getCLIOPT()
	{
		return new CommandLineTargetAlgorithmEvaluatorOptions();
	}
	public static TargetAlgorithmEvaluator getCLITAE(AlgorithmExecutionConfig config, CommandLineTargetAlgorithmEvaluatorOptions opts)
	{
		return new CommandLineTargetAlgorithmEvaluator(config, opts );
	}

	public static TargetAlgorithmEvaluator getCLITAE(AlgorithmExecutionConfig config)
	{
		
		CommandLineTargetAlgorithmEvaluatorOptions opts = new CommandLineTargetAlgorithmEvaluatorOptions();
		opts.logAllCallStrings = true;
		opts.logAllProcessOutput = true;
		return new CommandLineTargetAlgorithmEvaluator(config, opts );
	}
	
	public static TargetAlgorithmEvaluator getCLITAE(AlgorithmExecutionConfig config, int observerFrequency)
	{
		CommandLineTargetAlgorithmEvaluatorOptions options = new CommandLineTargetAlgorithmEvaluatorOptions();
		options.observerFrequency = observerFrequency;
		options.logAllProcessOutput = true;
		return new CommandLineTargetAlgorithmEvaluator(config,options);
	}
	
}

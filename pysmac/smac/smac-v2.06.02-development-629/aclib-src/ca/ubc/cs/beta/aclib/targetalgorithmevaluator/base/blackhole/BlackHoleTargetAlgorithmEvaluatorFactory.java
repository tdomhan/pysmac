package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.blackhole;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class BlackHoleTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory {

	@Override
	public String getName() {
		return "BLACKHOLE";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig, AbstractOptions options) {

		return new BlackHoleTargetAlgorithmEvaluator(execConfig, (BlackHoleTargetAlgorithmEvaluatorOptions) options);
	}

	@Override
	public BlackHoleTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new BlackHoleTargetAlgorithmEvaluatorOptions();
		
	}

}

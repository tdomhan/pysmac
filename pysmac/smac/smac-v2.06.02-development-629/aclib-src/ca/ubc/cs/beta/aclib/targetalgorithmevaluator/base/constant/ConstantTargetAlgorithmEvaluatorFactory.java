package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.constant;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class ConstantTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory 
{

	@Override
	public String getName() {
		return "CONSTANT";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig, AbstractOptions options) {
		return new ConstantTargetAlgorithmEvaluator(execConfig, (ConstantTargetAlgorithmEvaluatorOptions) options);
	}

	@Override
	public AbstractOptions getOptionObject() {
		return new ConstantTargetAlgorithmEvaluatorOptions();
	}

}

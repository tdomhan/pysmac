package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.analytic;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.SimulatedDelayTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)

public class AnalyticTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  {

	@Override
	public String getName() {
		return "ANALYTIC";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig, AbstractOptions options) {
		
		AnalyticTargetAlgorithmEvaluatorOptions analyticOptions = (AnalyticTargetAlgorithmEvaluatorOptions) options;
		
		TargetAlgorithmEvaluator tae = new AnalyticTargetAlgorithmEvaluator(execConfig, analyticOptions.func);
		
		if(analyticOptions.simulateDelay)
		{
			tae = new SimulatedDelayTargetAlgorithmEvaluatorDecorator(tae, analyticOptions.observerFrequency, analyticOptions.scaleDelay);
		}
		
		if(analyticOptions.cores > 0)
		{
			tae = new BoundedTargetAlgorithmEvaluator(tae, analyticOptions.cores, execConfig);
		}
		
		return tae;
	}

	@Override
	public AnalyticTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new AnalyticTargetAlgorithmEvaluatorOptions();
	}

}

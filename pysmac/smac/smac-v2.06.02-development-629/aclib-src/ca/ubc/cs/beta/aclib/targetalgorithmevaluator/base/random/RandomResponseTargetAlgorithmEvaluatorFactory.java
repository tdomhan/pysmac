package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.random;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.SimulatedDelayTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class RandomResponseTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  
{

	@Override
	public String getName() {
		return "RANDOM";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig, AbstractOptions options) {
		RandomResponseTargetAlgorithmEvaluatorOptions randomOptions = (RandomResponseTargetAlgorithmEvaluatorOptions) options;
		
		TargetAlgorithmEvaluator tae =  new RandomResponseTargetAlgorithmEvaluator(execConfig,randomOptions);
		
		if(randomOptions.simulateDelay)
		{
			tae = new SimulatedDelayTargetAlgorithmEvaluatorDecorator(tae, randomOptions.observerFrequency, randomOptions.scaleDelay);
		}
		
		if(randomOptions.cores > 0)
		{
			tae = new BoundedTargetAlgorithmEvaluator(tae, randomOptions.cores, execConfig);
		}
		
		return tae;
		
	}

	@Override
	public RandomResponseTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new RandomResponseTargetAlgorithmEvaluatorOptions();
	}
	
	

}

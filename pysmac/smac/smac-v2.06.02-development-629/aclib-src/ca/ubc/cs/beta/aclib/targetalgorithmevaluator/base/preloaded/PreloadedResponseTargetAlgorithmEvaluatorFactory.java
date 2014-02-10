package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.preloaded;

import java.util.LinkedList;
import java.util.Queue;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.associatedvalue.AssociatedValue;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class PreloadedResponseTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory {

	@Override
	public String getName() {
		return "PRELOADED";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig,AbstractOptions obs) {
		
		
		Queue<AssociatedValue<RunResult, Double>> myQueue = new LinkedList<AssociatedValue<RunResult, Double>>();
	
		PreloadedResponseTargetAlgorithmEvaluatorOptions opts = (PreloadedResponseTargetAlgorithmEvaluatorOptions) obs;
		
		for(String response : opts.preloadedResponses.split(","))
		{
			
			response = response.trim();
			if(response.length() == 0) continue;
			if(response.length() < 2) throw new IllegalArgumentException("Invalid Preloaded Response: " + response);
			if(!response.substring(0,1).equals("[")) throw new IllegalArgumentException("Invalid Preloaded Response: " + response);
			if(!response.substring(response.length()-1).equals("]")) throw new IllegalArgumentException("Invalid Preloaded Response: " + response);
			
			response = response.substring(1, response.length() - 1);
			String[] val = response.split("=");
			if(val.length != 2)
			{
				throw new IllegalArgumentException("Invalid Preloaded Response: " + response);
			}
			RunResult result = RunResult.getAutomaticConfiguratorResultForKey(val[0].trim());
			double value = Double.valueOf(val[1].trim());
			
			myQueue.add(new AssociatedValue<RunResult,Double>(result, value));
		}
		
		
		
		return new PreloadedResponseTargetAlgorithmEvaluator(execConfig, myQueue, opts);
	}



	@Override
	public AbstractOptions getOptionObject() {
		return new PreloadedResponseTargetAlgorithmEvaluatorOptions();
	}

}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.constant;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

public class ConstantTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator {

	private final ConstantTargetAlgorithmEvaluatorOptions options;
	
	public ConstantTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig, ConstantTargetAlgorithmEvaluatorOptions options) {
		super(execConfig);
		this.options = options;
		
		
		
	}

	@Override
	public boolean isRunFinal() {
		return true;
	}

	@Override
	public boolean areRunsPersisted() {
		return true;
	}

	@Override
	protected void subtypeShutdown() {
		
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs,
			TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRun> runs = new ArrayList<AlgorithmRun>();
		
		for(RunConfig rc : runConfigs)
		{
			String addlRunData = "";
			
			if((options.additionalRunData != null) && (options.additionalRunData.trim().length() > 0))
			{
				addlRunData = "," + options.additionalRunData;
			}
			
			runs.add(new ExistingAlgorithmRun(execConfig, rc, options.runResult + "," + options.runtime + "," + options.runlength + "," + options.quality + "," + rc.getProblemInstanceSeedPair().getSeed() + addlRunData));
		}
		
		return runs;
	}

}

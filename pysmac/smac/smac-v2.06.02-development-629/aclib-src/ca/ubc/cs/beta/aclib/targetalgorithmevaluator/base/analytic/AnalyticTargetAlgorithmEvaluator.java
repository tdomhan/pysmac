package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.analytic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;

public class AnalyticTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator implements
		TargetAlgorithmEvaluator {

	private final AnalyticFunctions func;

	private final Logger log = LoggerFactory.getLogger(getClass());
	public AnalyticTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig, AnalyticFunctions func) {
		super(execConfig);
		this.func = func;
	}

	@Override
	public boolean isRunFinal() {
		return true;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

	@Override
	protected void subtypeShutdown() {
		log.info("Global minima for {} function are near {}", func.name() ,  func.getMinima());
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs,
			TargetAlgorithmEvaluatorRunObserver obs) {
		try{
			
			List<AlgorithmRun> ar = new ArrayList<AlgorithmRun>(runConfigs.size());
			
			for(RunConfig rc : runConfigs)
			{ 
				
				List<Double> vals = new ArrayList<Double>();
				
				for(int i=0; i < 1000; i++)
				{
					if(rc.getParamConfiguration().containsKey("x" + i))
					{
						vals.add(Double.valueOf(rc.getParamConfiguration().get("x" + i)));
					}
				}
				
				double time = func.evaluate(vals);
				
				for(String key : rc.getParamConfiguration().keySet())
				{
					if(key.matches("x[0-9]+"))
					{
						continue;
					}
					
					Calculable calc = new ExpressionBuilder(key).withVariable("X", Double.valueOf(rc.getParamConfiguration().get(key))).build();
					time+=calc.calculate();
				}
				
				String instInfo = rc.getProblemInstanceSeedPair().getInstance().getInstanceSpecificInformation();
				if(instInfo.startsWith("Analytic-Instance-Cost:"))
				{
					try {
						time += Double.valueOf(instInfo.replace("Analytic-Instance-Cost:", ""));
					} catch(NumberFormatException e)
					{
						throw new NumberFormatException("Couldn't parse analytic instance cost from instance: " + rc.getProblemInstanceSeedPair().getInstance());
					}
				}
								
				if(time >= rc.getCutoffTime())
				{
					ar.add(new ExistingAlgorithmRun(execConfig, rc, RunResult.TIMEOUT,  rc.getCutoffTime() ,-1,0, rc.getProblemInstanceSeedPair().getSeed()));
				} else
				{
					ar.add(new ExistingAlgorithmRun(execConfig, rc, RunResult.SAT,  time ,-1,0, rc.getProblemInstanceSeedPair().getSeed()));
				}
				this.runCount.incrementAndGet();
			}
			return ar;
		}
		catch(RuntimeException e){
			
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		} catch (UnknownFunctionException e) {
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		} catch (UnparsableExpressionException e) {
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		}
	}
	

}

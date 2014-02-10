package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.safety;

import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * TAE Decorator that checks that SAT/UNSAT answers match on repeated runs on the same problem instance
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class SATConsistencyTargetAlgorithmEvaluator extends AbstractForEachRunTargetAlgorithmEvaluatorDecorator
{

	private final ConcurrentHashMap<ProblemInstance, RunResult> runResults = new ConcurrentHashMap<ProblemInstance, RunResult>();
	private final static Logger log =  LoggerFactory.getLogger(SATConsistencyTargetAlgorithmEvaluator.class);
	private final boolean throwException;
	
	public SATConsistencyTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae, boolean throwException) {
		super(tae);
		this.throwException = throwException;
		
	}

	@Override
	protected AlgorithmRun processRun(AlgorithmRun run) {
		RunResult result = run.getRunResult();
		
		switch(result)
		{
			case SAT:
			case UNSAT:
				RunResult previousResult = runResults.putIfAbsent(run.getRunConfig().getProblemInstanceSeedPair().getInstance(), result);
				if(previousResult != null)
				{
					if(!previousResult.equals(result))
					{
						Object[] args = { run.getRunConfig().getProblemInstanceSeedPair().getInstance(), previousResult, result}; 
						log.error("SAT/UNSAT discrepancy detected on problem instance: {}. Previous value: {}, currentValue: {}" , args);
						if(throwException)
						{
							throw new TargetAlgorithmAbortException("RunResult dispatch detected inconsistence with previous result " + previousResult + " currentResult: " + result + " run" + run);
						}
					}
				}
				
			case TIMEOUT:
			case CRASHED:
			case ABORT:
			case KILLED:
				break;
			default:
				throw new IllegalStateException("Unexpected run result on algorithm run: " + run);
		}
		
		return run;
	}
}
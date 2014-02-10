package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;

import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ec.util.MersenneTwister;

/***
 * Random Target Algorithm Evaluator
 * 
 * Generates random responses to Run Configs
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class RandomResponseTargetAlgorithmEvaluator extends
		AbstractSyncTargetAlgorithmEvaluator {

	private final double scale;
	private final double trendCoefficient;
	private final double minValue;
	
	
	private boolean persistent;
	//Controls whether we will BREAK our TAE by shuffling the runs
	private boolean shuffleRuns;
	
	private final Random rand;
	private final String additionalRunData;
	
	private static final Logger log = LoggerFactory.getLogger(RandomResponseTargetAlgorithmEvaluator.class);
			
	public RandomResponseTargetAlgorithmEvaluator (
			AlgorithmExecutionConfig execConfig, RandomResponseTargetAlgorithmEvaluatorOptions options) {
		super(execConfig);
		
		
		if(options.maxResponse - options.minResponse < 0)
		{
			throw new ParameterException("Maximum response must be greater than the minimum response");
		}
		this.scale = options.maxResponse - options.minResponse;
		this.minValue = options.minResponse;
		

		this.trendCoefficient = options.trendCoefficient;

		log.info("Target Algorithm Evaluator initialized with seed: {} ", options.seed);
		this.rand = new MersenneTwister(options.seed);
		this.shuffleRuns = options.shuffleResponses;
		this.persistent = options.persistent;
		this.additionalRunData = options.additionalRunData;

	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRun> ar = new ArrayList<AlgorithmRun>(runConfigs.size());
		
		for(RunConfig rc : runConfigs)
		{ 
			double time = Math.max(0.01, ((rand.nextDouble()*this.scale)  + this.minValue) + (this.trendCoefficient * this.getRunCount()));
			
			if(time >= rc.getCutoffTime())
			{
				ar.add(new ExistingAlgorithmRun(execConfig, rc, RunResult.TIMEOUT,  rc.getCutoffTime() ,-1,0, rc.getProblemInstanceSeedPair().getSeed(), this.additionalRunData));
			} else
			{
				ar.add(new ExistingAlgorithmRun(execConfig, rc, RunResult.SAT,  time ,-1,0, rc.getProblemInstanceSeedPair().getSeed(), this.additionalRunData));
			}
			this.runCount.incrementAndGet();
		}
		
		if(shuffleRuns)
		{
			Collections.shuffle(ar, rand);
		}
		return ar;
	}

	@Override
	public boolean isRunFinal() {
		return false;
	}

	@Override
	public boolean areRunsPersisted() {
		return persistent;
	}

	@Override
	protected void subtypeShutdown() {
		
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators;

import java.util.Collections;
import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
/**
 * Abstract Decorator class for {@link ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator}
 * 
 * <br/>
 * <b>Implementation Note:</b>  Almost every decorator that is doing something interesting, will
 * in fact redirect evaluateRun(RunConfig) to it's own local evaluateRun(List<RunConfig>) method.
 * You should not rely on evaluateRun() being called directly.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public abstract class AbstractTargetAlgorithmEvaluatorDecorator implements	TargetAlgorithmEvaluator {

	protected final TargetAlgorithmEvaluator tae;

	public AbstractTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae)
	{
		this.tae = tae;
		
	}
	
	
	@Override
	public final List<AlgorithmRun> evaluateRun(RunConfig runConfig) {
		return evaluateRun(Collections.singletonList(runConfig));
	}

	@Override
	public final List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		return evaluateRun(runConfigs, null);
	}


	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver observer) {
		return tae.evaluateRun(runConfigs, observer);
	}

	@Override
	public final void evaluateRunsAsync(RunConfig runConfig, TargetAlgorithmEvaluatorCallback callback) {
		evaluateRunsAsync(Collections.singletonList(runConfig), callback);
	}

	@Override
	public final void evaluateRunsAsync(List<RunConfig> runConfigs, final TargetAlgorithmEvaluatorCallback callback) {
		evaluateRunsAsync(runConfigs, callback, null);
	}

	
	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) {
		tae.evaluateRunsAsync(runConfigs, callback, observer);
	}

	
	public void waitForOutstandingEvaluations()
	{
		tae.waitForOutstandingEvaluations();
	}
	
	
	public int getNumberOfOutstandingEvaluations()
	{
		return tae.getNumberOfOutstandingBatches();
	}
	
	
	
	@Override
	public int getRunCount() {
		return tae.getRunCount();
	}

	@Override
	public int getRunHash() {
		return tae.getRunHash();
	}

	@Override
	public void seek(List<AlgorithmRun> runs) {
		tae.seek(runs);

	}
	@Override
	public String getManualCallString(RunConfig runConfig) {
		return tae.getManualCallString(runConfig);
	}
	
	@Override
	public void notifyShutdown()
	{
		tae.notifyShutdown();
	}
	
	@Override
	public boolean isRunFinal()
	{
		return tae.isRunFinal();
	}
	
	@Override
	public boolean areRunsPersisted()
	{
		return tae.areRunsPersisted();
	}
	
	@Override
	public boolean areRunsObservable()
	{
		return tae.areRunsObservable();
	}
	
	@Override
	public final String toString()
	{
		return this.getClass().getSimpleName() + "( 0x" + Integer.toHexString(System.identityHashCode(this)) + " ) ==> [ " + tae.toString() + " ]";
	}


	@Override
	public int getNumberOfOutstandingBatches() {
		return tae.getNumberOfOutstandingBatches();
	}


	@Override
	public int getNumberOfOutstandingRuns() {
		return tae.getNumberOfOutstandingRuns();
	}
	
	

}

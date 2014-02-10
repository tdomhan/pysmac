package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * Abstract TargetAlgorithmEvaluator that implements a basic form of asynchronous execution.
 * <br>
 * <b>Note:</b> Calls will just be made in a separate thread  
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public abstract class AbstractSyncTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluator {

	ExecutorService execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Abstract Blocking TAE Async Processing Thread"));
	private final Logger log = LoggerFactory.getLogger(getClass());
	public AbstractSyncTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig) {
		super(execConfig);
	}

	@Override
	public  void evaluateRunsAsync(final List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		Runnable run = new Runnable()
		{

			@Override
			public void run() {
				
				try {
					List<AlgorithmRun> runs = AbstractSyncTargetAlgorithmEvaluator.this.evaluateRun(runConfigs, obs);
					
					handler.onSuccess(runs);
				} catch(RuntimeException e)
				{
					handler.onFailure(e);
				} catch(Throwable t)
				{
					log.error("Uncaught throwable occured ", t);
				}
			}
			
		};
		
		
		
		if(this.areRunsPersisted())
		{
			//Need to ensure that the runs get checked for being done.
			//I don't remember why this case is here and I don't think anything ever
			//returns true that implements this.
			run.run();
		} else
		{
			execService.execute(run);
		}

	}

	/**
	 * Template method for ensuring subtype gets notified. 
	 */
	protected abstract void subtypeShutdown();
	
	
	/**
	 * We must be notified of the shutdown, so we will prevent subtypes from overriding this method.
	 */
	@Override
	public final void notifyShutdown()
	{
		execService.shutdown();
		this.subtypeShutdown();
	}
}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.resource;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;

/**
 * This decorator ensures that the {@link #evaluateRunsAsync()} method's never block. Obviously {@link #evaluateRun()} will still block.
 * 
 * This can be useful when you do not want more than a certain amount of runs 
 * to go past a certain point, but don't actually want to wait for them.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class NonBlockingAsyncTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	private final ExecutorService execService = Executors.newSingleThreadExecutor(new SequentiallyNamedThreadFactory(getClass() + " Processor", true));
			
	private final BlockingQueue<Triple> queue = new LinkedBlockingQueue<Triple>();
	
	public NonBlockingAsyncTargetAlgorithmEvaluatorDecorator(final TargetAlgorithmEvaluator tae) {
		super(tae);
		
		execService.execute(new Runnable()
		{

			@Override
			public void run() {
				
				
				try {
					while(true)
					{
						Triple t;
						t = queue.take();
						tae.evaluateRunsAsync(t.runConfigs, t.callback, t.observer);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				
				
				
			}
			
		});
	}

	@Override
	public void notifyShutdown()
	{
		
		tae.notifyShutdown();
		execService.shutdownNow();
		
	}

	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) {
		try {
			queue.put(new Triple(runConfigs, callback, observer));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}


	@Override
	public void waitForOutstandingEvaluations()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, even if the wrapped class does, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, even if the wrapped class does, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	
	private class Triple
	{
		List<RunConfig> runConfigs;
		TargetAlgorithmEvaluatorCallback callback;
		TargetAlgorithmEvaluatorRunObserver observer;
		
		public Triple(List<RunConfig> runConfigs2,
				TargetAlgorithmEvaluatorCallback callback2,
				TargetAlgorithmEvaluatorRunObserver observer2) {
			
			this.runConfigs = runConfigs2;
			this.callback = callback2;
			this.observer = observer2;
				
		}
		
		
	}
	
	
	
}

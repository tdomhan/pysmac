package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AbstractAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

public class WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final double wallclockMultScaleFactor;
	private final double startAt;
	public WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
		wallclockMultScaleFactor = 0.95;
		startAt = 0.05;
	}
	
	public WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae, double scaleFactor, double startAt) {
		super(tae);
		wallclockMultScaleFactor = scaleFactor;
		this.startAt = startAt;
	}
	
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * Stores runs we have already killed in a weak map so that they can be garbage collected if need be.
	 * The synchronization here is for memory visibility only, it doesn't
	 *
	 */
	

	@Override
	public final List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return processRuns(tae.evaluateRun(runConfigs, new WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(obs)));
	}
	
	
	
	
	
	
	public List<AlgorithmRun> processRuns(List<AlgorithmRun> runs)
	{
		List<AlgorithmRun> myRuns = new ArrayList<AlgorithmRun>(runs.size());
		for(AlgorithmRun run : runs)
		{
			myRuns.add(processRun(run));
		}
		return myRuns;
	}
	
	public AlgorithmRun processRun(AlgorithmRun run )
	{
		if(run.getRunResult().equals(RunResult.KILLED))
		{
			if(run.getRuntime() == 0 && run.getWallclockExecutionTime() > startAt)
			{
		
				return new WalltimeAsRuntimeKillableAlgorithmRun(run);
			}
		}
		return run;
		
	}
	
	@Override
	public final void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		//We need to make sure wrapped versions are called in the same order
		//as there unwrapped versions.
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
					runs = processRuns(runs);			
					handler.onSuccess(runs);
			}

			@Override
			public void onFailure(RuntimeException t) {
					handler.onFailure(t);
			}
		};
		
		tae.evaluateRunsAsync(runConfigs, myHandler, new WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(obs));

	}

	


	private class WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private TargetAlgorithmEvaluatorRunObserver obs;
		WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
		}
		
		@Override
		public void currentStatus(List<? extends KillableAlgorithmRun> runs) 
		{
			
			List<KillableAlgorithmRun> myRuns = new ArrayList<KillableAlgorithmRun>(runs.size());
			
			for(KillableAlgorithmRun run : runs)
			{
				
				if(run.getRunResult().equals(RunResult.RUNNING))
				{
					if(run.getRuntime() == 0 && run.getWallclockExecutionTime() > startAt)
					{
				
						myRuns.add(new WalltimeAsRuntimeKillableAlgorithmRun(run));
						
					} else
					{
						myRuns.add(run);
					}
				} else
				{
					myRuns.add(run);
				}
			}
			if(obs != null)
			{
				obs.currentStatus(myRuns);
			}
		}
		
		
	}
	
	private class WalltimeAsRuntimeKillableAlgorithmRun implements KillableAlgorithmRun
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 9082975671200245863L;
		
		AlgorithmRun wrappedRun;
		KillableAlgorithmRun wrappedKillableRun;  
		public WalltimeAsRuntimeKillableAlgorithmRun(AlgorithmRun r)
		{
			if(r instanceof KillableAlgorithmRun)
			{
				wrappedKillableRun = (KillableAlgorithmRun) r;
			}
			this.wrappedRun = r;
		}
		
		

		@Override
		public AlgorithmExecutionConfig getExecutionConfig() {
			return wrappedRun.getExecutionConfig();
		}

		@Override
		public RunConfig getRunConfig() {
			return wrappedRun.getRunConfig();
		}

		@Override
		public RunResult getRunResult() {
			return wrappedRun.getRunResult();
		}

		@Override
		public double getRuntime() {
			return Math.max(wrappedRun.getWallclockExecutionTime() * wallclockMultScaleFactor,0);
		}

		@Override
		public double getRunLength() {
			return wrappedRun.getRunLength();
		}

		@Override
		public double getQuality() {
			return wrappedRun.getQuality();
		}

		@Override
		public long getResultSeed() {
			return wrappedRun.getResultSeed();
		}

		@Override
		public String getResultLine() {
			return AbstractAlgorithmRun.getResultLine(this);
		}

		@Override
		public String getAdditionalRunData() {
			return wrappedRun.getAdditionalRunData();
		}

		@Override
		public void run() {
			wrappedRun.run();
			
		}

		@Override
		public Object call() {
			return wrappedRun.call();
		}

		@Override
		public boolean isRunCompleted() {
			return wrappedRun.isRunCompleted();
		}

		@Override
		public boolean isRunResultWellFormed() {
			return wrappedRun.isRunResultWellFormed();
		}

		@Override
		public String rawResultLine() {
			return "[Probably not accurate:]" + wrappedRun.rawResultLine();
		}

		@Override
		public double getWallclockExecutionTime() {
			return wrappedRun.getWallclockExecutionTime();
		}

		@Override
		public void kill() {
			if(wrappedKillableRun != null)
			{
				wrappedKillableRun.kill();
				
			}			
		}
		
		public String toString()
		{
			return AbstractAlgorithmRun.toString(this);
		}
		
	}

}

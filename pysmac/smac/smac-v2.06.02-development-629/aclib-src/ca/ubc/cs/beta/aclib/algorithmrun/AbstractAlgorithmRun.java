package ca.ubc.cs.beta.aclib.algorithmrun;

import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.exceptions.IllegalWrapperOutputException;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.watch.StopWatch;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * This class represents a single run of the target algorithm given by the AlgorithmExecutionConfig object and the RunConfig object
 * 
 * @author seramage
 *
 */
public abstract class AbstractAlgorithmRun implements Runnable, AlgorithmRun{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1860615761848618478L;
	
	protected final RunConfig runConfig;
	protected final AlgorithmExecutionConfig execConfig;
	
	/*
	 * Values reported by the target algorithm
	 */
	private RunResult acResult;
	
	private double runtime;
	private double runLength;
	private double quality;
	private long resultSeed; 
	
	/**
	 * Raw result line reported by the target algorithm (potentially useful if the result line is corrupt)
	 */
	private String rawResultLine;
		
	/**
	 * true if the result has been set,
	 * if this is false most methods will throw an IllegalStateException
	 */
	
	private boolean resultSet = false;
	/**
	 * True if the run was well formed
	 * Note: We may deprecate this in favor of using CRASHED
	 */
	private boolean runResultWellFormed = false;
	
	/**
	 * Wallclock Time to return
	 */
	private double wallClockTime = 0;
	
	/**
	 * Watch that can be used to time algorithm runs 
	 */
	private	StopWatch wallClockTimer = new StopWatch();
	
	/**
	 * Stores additional run data
	 */
	private String additionalRunData = "";
	
	/**
	 * Sets the values for this Algorithm Run
	 * @param acResult					The result of the Run
	 * @param runtime					Reported runtime of the run
	 * @param runLength					Reported runlength of the run
	 * @param quality					Reported quality of the run
	 * @param resultSeed				Reported seed of the run
	 * @param rawResultLine				The Raw result line we got
	 * @param additionalRunData			Additional Run Data
	 */
	protected synchronized void setResult(RunResult acResult, double runtime, double runLength, double quality, long resultSeed, String rawResultLine, String additionalRunData)
	{
		this.setResult(acResult, runtime, runLength, quality, resultSeed, rawResultLine, true, additionalRunData);
	}
	
	/**
	 * Marks this run as aborted
	 * @param rawResultLine  the raw output that might be relevant to this abort
	 */
	protected void setAbortResult(String rawResultLine)
	{
		this.setResult(RunResult.ABORT, runConfig.getCutoffTime(), 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine, "");
	}
	
	/**
	 * Marks this run as aborted
	 * @param rawResultLine  the raw output that might be relevant to this crash
	 */
	protected void setCrashResult(String rawResultLine)
	{
		this.setResult(RunResult.CRASHED, runConfig.getCutoffTime(), 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine,rawResultLine);
	}
	

	protected void startWallclockTimer()
	{
		wallClockTimer.start();
	}
	
	protected void stopWallclockTimer()
	{
		this.wallClockTime = wallClockTimer.stop() / 1000.0;
	}
	
	protected long getCurrentWallClockTime()
	{
		return this.wallClockTimer.time();
	}
	/**
	 * Sets the values for this Algorithm Run
	 * @param acResult					RunResult for this run
	 * @param runtime					runtime measured
	 * @param runLength					runlength measured
	 * @param quality					quality measured
	 * @param resultSeed				resultSeed 
	 * @param rawResultLine				raw result line
	 * @param runResultWellFormed		whether this run has well formed output
	 * @param additionalRunData			additional run data from this run
	 */
	protected synchronized void setResult(RunResult acResult, double runtime, double runLength, double quality, long resultSeed , String rawResultLine, boolean runResultWellFormed, String additionalRunData)
	{
		if(Double.isNaN(runtime) || runtime < 0)
		{
			throw new IllegalWrapperOutputException("Runtime is NaN or negative", rawResultLine);
		}
			
		if ( Double.isNaN(runLength) || ((runLength < 0) && (runLength != -1.0)))
		{
			throw new IllegalWrapperOutputException("RunLength (" + runLength + ") is NaN or negative (and not -1)", rawResultLine);
		}
		
		if(Double.isNaN(quality))
		{
			throw new IllegalWrapperOutputException("Quality needs to be a number", rawResultLine);
		}
		
		if(acResult == null)
		{
			throw new IllegalStateException("Run Result cannot be null");
		}
		this.acResult = acResult;
		this.runtime = Math.min(runtime, Double.MAX_VALUE);
		this.runLength = Math.min(runLength, Double.MAX_VALUE);
		this.quality = quality;
		this.resultSeed = resultSeed;
		
		if(this.saveRawResultLine())
		{
			this.rawResultLine = rawResultLine;
		}
		
		this.runResultWellFormed = runResultWellFormed;

		this.additionalRunData = additionalRunData;
		if(this.additionalRunData == null)
		{
			throw new IllegalArgumentException("Additional Run Data cannot be NULL");
		}
		this.resultSet = true;
		if(!(this instanceof KillableAlgorithmRun))
		{
			
			if(this.acResult.equals(RunResult.RUNNING))
			{
				throw new IllegalStateException("Only " + KillableAlgorithmRun.class.getSimpleName() + " may be set as " + RunResult.RUNNING);
			}
		}
		
		
	}
	
	
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 */
	public AbstractAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig)
	{
		if(execConfig == null || runConfig == null)
		{
			throw new IllegalArgumentException("Arguments cannot be null");
		}
		
		this.runConfig = runConfig;
		this.execConfig = execConfig;
	}
	
	@Override
	public abstract void run();

	/**
	 * Synonym of {@link AbstractAlgorithmRun#run()}
	 * <p>
	 * <b>Implementation Note:</b> If there is a good reason this method can be made non final 
	 * but as a rule call should be the same a run().
	 * 
	 *  @return null
	 */
	@Override
	public final Object call()
	{
		run();
		return null;
	}
	
	@Override
	public final AlgorithmExecutionConfig getExecutionConfig()
	{
		return execConfig;
	}
	
	@Override
	public final RunConfig getRunConfig()
	{
		return runConfig;
	}
	
	
	@Override
	public final RunResult getRunResult() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return acResult;
	}

	@Override
	public final double getRuntime() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return runtime;
	}

	@Override
	public final double getRunLength() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return runLength;
	}

	@Override
	public final double getQuality() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return quality;
	}

	@Override
	public final long getResultSeed() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return resultSeed;
	}
	
	private final String _getResultLine()
	{
		return getResultLine(this);
	}
	
	public static final String getResultLine(AlgorithmRun run)
	{
		String resultLine = run.getRunResult().name() + ", " + run.getRuntime() + ", " + run.getRunLength() + ", " + run.getQuality() + ", " + run.getResultSeed();
		if(run.getAdditionalRunData().trim().length() > 0)
		{
			resultLine += "," + run.getAdditionalRunData();
		}
		return resultLine;
	}
	
	@Override
	public final String getResultLine() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return _getResultLine();
	}

	@Override
	public final synchronized boolean isRunCompleted() {
		if(acResult == null)
		{
			return false;
		} else
		{
			return !acResult.equals(RunResult.RUNNING);
		}
	}

	@Override
	public final synchronized boolean isRunResultWellFormed() {
		if(!isResultSet()) throw new IllegalStateException("Run has not yet completed: " + this.toString());
		return runResultWellFormed;
	}
	
	@Override
	public final String rawResultLine()
	{
		if(!isResultSet()) throw new IllegalStateException("Run has not yet completed: " + this.toString());
		
		if(saveRawResultLine())
		{
			return rawResultLine;
		} else
		{
			return "[Raw Result Line Not Saved]";
		}
		
	}
	
	@Override
	public final int hashCode()
	{
		//I believe that just instanceConfig and not execConfig hashCodes should be good enough
		//Since it's rare for two different execConfigs to have identical instanceConfigs
		return runConfig.hashCode();
	}
	
	/**
	 * Two AlgorithmRuns are considered equal if they have same runConfig and execConfig
	 */
	@Override
	public final boolean equals(Object o)
	{
		if(this == o) return true;
		if(o == null) return false;
		
		if(o instanceof AlgorithmRun)
		{
			AlgorithmRun aro = (AlgorithmRun) o;
			return aro.getExecutionConfig().equals(execConfig) && aro.getRunConfig().equals(runConfig);
		} 
		return false;
	}
	
	@Override 
	public String toString()
	{
		return toString(this);
	}
	
	
	public static String toString(AlgorithmRun run)
	{
		return run.getRunConfig().toString() + " ==> <" + run.getResultLine()+ "> W:(" + run.getWallclockExecutionTime() + ")";
		
	}
	/**
	 * Sets the wallclock time for this target algorithm
	 * @param time time in seconds that the algorithm executed
	 */
	protected void setWallclockExecutionTime(double time)
	{
		if(time < 0) throw new IllegalArgumentException("Time must be positive");
		this.wallClockTime = time;
	}
	
	@Override
	public double getWallclockExecutionTime() {
		return wallClockTime;
	}
	
	@Override
	public String getAdditionalRunData() {
	
		return additionalRunData;
	}
	
	protected boolean isResultSet()
	{
		return resultSet;
	}
	
	private boolean saveRawResultLine()
	{
		return false;
	}
	
	
}

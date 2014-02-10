package ca.ubc.cs.beta.aclib.algorithmrun;

import java.io.Serializable;
import java.util.concurrent.Callable;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * Represents an execution of a target algorithm. 
 * 
 * All implementations should be effectively immutable (except for the run()) method that is.
 * 
 * NOTE: The following invariants exist, and implementations that don't follow this may have unexpected results
 * 
 * @author sjr
 */
public interface AlgorithmRun extends Runnable, Serializable,  Callable<Object> {

	/**
	 * Returns the AlgorithmExecutionConfig of the run
	 * 
	 * @return AlgorithmExecutionConfig of the run
	 * 
	 */
	public AlgorithmExecutionConfig getExecutionConfig();

	/**
	 * Return the run configuration associated with the AlgorithmRun
	 * @return run configuration of this run
	 */
	public RunConfig getRunConfig();

	/**
	 * Get the Run Result
	 * 
	 * <b>Implementation Notes:</b>
	 * 
	 *  The Run Result should be TIMEOUT if the cutoff time is zero, and implementations may not do anything else but return this run. 
	 *  
	 *  The Run Result should NEVER be RUNNING, unless this is an appropriate subtype that supports Killing.
	 *  
	 *  If the result is RUNNING then isRunComplete() should return <code>false</code> otherwise it should return </code>true</code>
	 *  
	 * @return RunResult for run
	 * @throws IllegalStateException if the run has not completed
	 */
	public RunResult getRunResult();

	/**
	 * Get reported runtime of run 
	 * 
	 * @return double for the runtime (>= 0) && < Infinity
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getRuntime();

	/**
	 * Get the reported run length
	 * 
	 * @return double for the runlength ( >= 0 && < Infinity) || -1 
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getRunLength();

	/**
	 * Get the reported quality 
	 * 
	 * @return double for the quality ( > -Infinity && < +Infinity)
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getQuality();

	/**
	 * Get the seed that was returned
	 * 
	 * NOTE: For well behaved programs this should always be the seed in the ProblemInstanceSeedPair of RunConfig
	 * @return seed reported by algorithm
	 * @throws IllegalStateException if the run has not completed
	 */
	public long getResultSeed();

	/**
	 * Note: This should always return a well-formatted result line. It may NOT necessarily correspond to values
	 * that the methods return. 
	 * 
	 * (i.e. You should be able to use this output as standard output without any validation, but it may not correspond to what we got this time)
	 * 
	 * Some extreme examples are when we clean up messy wrappers output (for instance SAT >= timeout). Depending on how the cleanup is done, this
	 * may change the result flagged, or we may massage the timeout. 
	 * 
	 * @return string representing a close approximation of this run that is guaranteed to be parsable.
	 * @throws IllegalStateException if the run has not completed
	 */
	public String getResultLine();
	
	
	/**
	 * Returns a (comma free) String from the algorithm run with additional data
	 * 
	 * This data generally has no meaning for SMAC but should be saved and restored in the run history file
	 * 
	 * @return a string (possibly empty but never null) that has the additional run data in it. (This string will also generally be trimed())
	 * 
	 */
	public String getAdditionalRunData(); 
	
	/**
	 * Runs this AlgorithmRun
	 * 
	 * Subsequent calls to this should be noop, and are not error conditions.
	 * 
	 * If this method successfully returns it's guaranteed that isRunCompleted() is true
	 */
	public void run();
	
	/**
	 * Runs this Algorithm Run
	 * 
	 * Subsequent calls to this should be a noop, and are not error conditions
	 * 
	 * If this method successfully returns it's guaranteed that isRunCompleted is true
	 * 
	 * @return null (always)
	 */
	public Object call();
	

	/**
	 * Returns true if the run is complete
	 * <b>Implementation Note:</b>This should always be the same as run.getRunResult().equals(RunResult.RUNNING)
	 * 
	 * @return <code>true</code> if this run has finished executing, <code>false</code> otherwise
	 * 
	 * 
	 */
	public boolean isRunCompleted();

	/**
	 * Returns whether this run gave us intelligible output
	 * @return <code>true</code> if this run returned something parsable, <code>false</code> otherwise
	 * @throws IllegalStateException if the run has not completed
	 * NOTE: This method will probably go away in favor of having algorithms just use CRASHED as the result
	 */
	public boolean isRunResultWellFormed();

	/**
	 * Returns the raw output of the line we matched (if any), this is for debug purposes only
	 * and there is no requirement that this actually return any particular string.
	 * <p>
	 * <b>Implementation Note:</b> An example where this is useful is if you use a weaker regex to match a possible output, and 
	 * then the stronger parsing fails. The weaker regex match could be returned here
	 * 
	 * @return string possibly containing a raw result
	 */
	public abstract String rawResultLine();
	
	/**
	 * Returns the amount of wallclock time the algorithm executed for
	 * <p>
	 * <b>Implementation Note:</b> This is NOT the runtime of the reported algorithm and may be less than or greater than in certain circumstances
	 * <p>
	 * In cases where the algorithm can determine that it won't solve the algorithm in a given time it may very well be less than the reported RunTime
	 * <p>
	 * In cases where the algorithm has a lot of overhead this may be drastically higher than the algorithm reports.
	 * 
	 * @return amount of time in seconds the algorithm ran for in seconds
	 */
	public double getWallclockExecutionTime();
	
	

}
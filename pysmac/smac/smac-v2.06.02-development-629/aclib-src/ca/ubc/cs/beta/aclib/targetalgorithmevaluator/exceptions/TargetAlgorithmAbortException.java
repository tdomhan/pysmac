package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions;

import java.io.IOException;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

/**
 * Exception thrown if a target algorithm signals an abort
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class TargetAlgorithmAbortException extends RuntimeException {

	private static final long serialVersionUID = 772736289871868435L;
	private AlgorithmRun run;

	public TargetAlgorithmAbortException(AlgorithmRun run)
	{
		super("Target algorithm execution signaled that we should ABORT " + run.rawResultLine());
		this.run = run;
	}
	
	public TargetAlgorithmAbortException(String message) {
		super(message);
	}
	
	public TargetAlgorithmAbortException(String message,Exception e)
	{
		super(message, e);
	}

	public TargetAlgorithmAbortException(InterruptedException e) {
		super("TargetAlgorithmEvaluator encountered an InterruptedException", e);
	}

	public TargetAlgorithmAbortException(IOException e1) {
		super("TargetAlgorithmEvaluator encounted an IOException ", e1);
	}

	public AlgorithmRun getAlgorithmRun()
	{
		return run;
	}
}

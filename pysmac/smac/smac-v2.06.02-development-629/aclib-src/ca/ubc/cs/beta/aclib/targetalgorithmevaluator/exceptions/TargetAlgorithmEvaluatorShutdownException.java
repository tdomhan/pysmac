package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions;

/**
 * Occurs when the TAE has been shutdown, or when the current thread has been interrupted
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class TargetAlgorithmEvaluatorShutdownException extends RuntimeException {

	
	private static final long serialVersionUID = 6812069375285515103L;

	public TargetAlgorithmEvaluatorShutdownException(Exception e) {
		super(e);
	}

}

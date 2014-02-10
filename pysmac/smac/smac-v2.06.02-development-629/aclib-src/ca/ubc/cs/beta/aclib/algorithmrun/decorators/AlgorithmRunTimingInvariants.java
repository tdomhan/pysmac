package ca.ubc.cs.beta.aclib.algorithmrun.decorators;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

/**
 * Corrects output from misbehaiving wrappers
 * 
 * Specifically it ensures that:
 * 
 * If Runtime >= Cutofftime, Result is TIMEOUT, and time is cap time.
 * 
 * 
 * 
 *
 * 
 * 
 * @author seramage
 *
 */
public class AlgorithmRunTimingInvariants extends AbstractAlgorithmRunDecorator {

	public AlgorithmRunTimingInvariants(AlgorithmRun run) {
		super(run);
	}

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4108923561335725916L;
	
	
	
	

	
}

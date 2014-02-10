package ca.ubc.cs.beta.aclib.exceptions;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;


public class OutOfTimeException extends SMACException {

	
	private final AlgorithmRun run;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3562273461188581045L;

	public OutOfTimeException(AlgorithmRun run) {
		super("SMAC is out of time.");
		this.run = run;
	}

	public OutOfTimeException() {
		super("Out of time");
		this.run = null;
		// TODO Auto-generated constructor stub
	}

	public AlgorithmRun getAlgorithmRun()
	{
		return run;
	}
}

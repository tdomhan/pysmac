package ca.ubc.cs.beta.aclib.exceptions;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

public class DuplicateRunException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6560952886591079120L;
	
	private final AlgorithmRun run;
	public DuplicateRunException(String message, AlgorithmRun r)
	{
		super(message +"\n"+ r.getRunConfig().toString());
		this.run = r;
		
	}

	public AlgorithmRun getRun()
	{
		return run;
	}
}

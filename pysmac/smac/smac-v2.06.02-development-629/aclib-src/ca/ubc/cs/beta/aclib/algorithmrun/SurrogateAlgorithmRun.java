package ca.ubc.cs.beta.aclib.algorithmrun;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * Surrogate Algorithm Run
 * 
 * <b>NOTE:</b> This class has nothing to do with executing a surrogate 
 * and is used when making a surrogate from RunHistory results, we convert runs into these objects
 * 
 * @author Steve Ramage <sjr@sjrx.net>
 *
 */
public class SurrogateAlgorithmRun extends AbstractAlgorithmRun {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2253548614421450243L;
	private static long seedValues = Long.MIN_VALUE;
	
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param response			response value to use
	 */
	public SurrogateAlgorithmRun(AlgorithmExecutionConfig execConfig,RunConfig runConfig, double response  )
	{
		super(execConfig, runConfig);
		this.setResult(RunResult.SAT, response, 0, 0, seedValues++, "No Real Line, just response used","");
	}

	@Override
	public void run() {
		//NO OP
	}

}

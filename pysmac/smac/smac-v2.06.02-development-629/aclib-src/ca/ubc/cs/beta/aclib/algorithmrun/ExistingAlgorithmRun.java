package ca.ubc.cs.beta.aclib.algorithmrun;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
/**
 * Class that is used to take an existing algorithm run (from for instance a string), and create an AlgorithmRun object
 * @author seramage
 */
public class ExistingAlgorithmRun extends AbstractAlgorithmRun {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7798477429606839878L;
	private transient Logger log = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 * @param wallclockTime		Wallclock time to report
	 */
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, RunResult runResult, double runtime, double runLength, double quality, long resultSeed, String additionalRunData, double wallclockTime)
	{
		super(execConfig, runConfig);
		this.setResult(runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", additionalRunData);
		this.setWallclockExecutionTime(wallclockTime);
	}
	
	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param wallclockTime		Wallclock time to report
	 */
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, RunResult runResult, double runtime, double runLength, double quality, long resultSeed,  double wallclockTime)
	{
		super(execConfig, runConfig);
		this.setResult(runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", "");
		this.setWallclockExecutionTime(wallclockTime);
	}
	
	
	

	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 */
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, RunResult runResult, double runtime, double runlength, double quality, long seed, String additionalRunData)
	{
		this(execConfig, runConfig, runResult, runtime,runlength, quality, seed, additionalRunData, 0.0);
	}
	

	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 */
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, RunResult runResult, double runtime, double runlength, double quality, long seed)
	{
		this(execConfig, runConfig, runResult, runtime,runlength, quality, seed, "", 0.0);
	}
	

	
	/**
	 * Default Constructor (sets Wallclock time to zero)
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param result			result string to parse. The format of this is currently everything after the : in the result line of {@link CommandLineAlgorithmRun}. We support both the String for the RunResult, as well as the Status Code
	 * @deprecated  the constructor that doesn't take a result string is preferred.
	 */
	@Deprecated
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, String result)
	{
		this(execConfig, runConfig, result, 0.0);
	}
	
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param result			result string to parse. The format of this is currently everything after the : in the result line of {@link CommandLineAlgorithmRun}. We support both the String for the RunResult, as well as the Status Code
	 * @deprecated  the constructor that doesn't take a result string is preferred. 
	 */
	@Deprecated
	public ExistingAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, String result, double wallClockTime) {
		super(execConfig, runConfig);
		//this.rawResultLine = resultLine;
		//this.runCompleted = true;
		String[] resultLine = result.split(",");
		
		try
		{
			RunResult acResult;
			try {
				acResult = RunResult.getAutomaticConfiguratorResultForCode(Integer.valueOf(resultLine[0]));
			} catch(NumberFormatException e)
			{
				acResult = RunResult.getAutomaticConfiguratorResultForKey(resultLine[0]);
			}
			
			
			double runtime = Double.valueOf(resultLine[1].trim());
			double runLength = Double.valueOf(resultLine[2].trim());
			double quality = Double.valueOf(resultLine[3].trim());
			long resultSeed = Long.valueOf(resultLine[4].trim());
			String additionalRunData = "";
			if(resultLine.length == 6)
			{
				additionalRunData = resultLine[5].trim();
			}
			
			
			this.setResult(acResult, runtime, runLength, quality, resultSeed, result, additionalRunData);
			
			
		} catch(ArrayIndexOutOfBoundsException e)
		{ 
			Object[] args = { execConfig, runConfig, result} ;
			
			log.info("Malformed Run Result for Execution (ArrayIndexOutOfBoundsException): {}, Instance: {}, Result: {}", args);
			log.info("Exception:",e);
			this.setAbortResult(e.getMessage());
		}catch(NumberFormatException e)
		{
			//There was a problem with the output, we just set this flag

			Object[] args = { execConfig, runConfig, result} ;
			log.info("Malformed Run Result for Execution (NumberFormatException): {}, Instance: {}, Result: {}", args);
			log.info("Exception:",e);
			this.setAbortResult( e.getMessage());
			
			
		}
		this.setWallclockExecutionTime(wallClockTime);
		

	}

	@Override
	public void run() {
		//NO OP

	}

}

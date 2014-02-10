package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;

@ThreadSafe
public class LogEveryTargetAlgorithmEvaluatorDecorator extends
		AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	Logger log = LoggerFactory.getLogger(LogEveryTargetAlgorithmEvaluatorDecorator.class);
	
	private final boolean logRCOnly;
	
	
	public LogEveryTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, boolean logRequestResponsesRCOnly) {
		super(tae);
		this.logRCOnly = logRequestResponsesRCOnly;
	}

	protected synchronized AlgorithmRun processRun(AlgorithmRun run)
	{
		if(logRCOnly)
		{
			log.debug("Run Completed: {} ", run.getRunConfig());
		} else
		{
			log.debug("Run Completed: {} ", run);
		}
		return run;
	}
	
	protected RunConfig processRun(RunConfig rc)
	{
		log.debug("Run Scheduled: {} ", rc);
		return rc;
	}

}

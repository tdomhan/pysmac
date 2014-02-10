package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.constant;

import java.util.Collections;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfigHelper;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.docgen.OptionsToUsage;
import ca.ubc.cs.beta.aclib.options.docgen.UsageSectionGenerator;
import ca.ubc.cs.beta.aclib.runconfig.RunConfigHelper;
import ca.ubc.cs.beta.aclib.smac.SMACOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class ConstantOptionsTest {

	/**
	 * @param args
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		
		
		ConstantTargetAlgorithmEvaluatorFactory fact = new ConstantTargetAlgorithmEvaluatorFactory();
		
		AbstractOptions consOpts = (AbstractOptions) fact.getOptionObject();
		
		Object[] args2 = { new SMACOptions(), consOpts}; 
		JCommander jcom = new JCommander(args2, true, true);
		jcom.addObject(consOpts);
		
		
		
		try {
			jcom.parse(args);
			
			TargetAlgorithmEvaluator tae = fact.getTargetAlgorithmEvaluator(AlgorithmExecutionConfigHelper.getSingletonExecConfig(), consOpts);
			
			for(int i=0; i < 1000; i++)
			{
				AlgorithmRun run = tae.evaluateRun(Collections.singletonList(RunConfigHelper.getRandomSingletonRunConfig())).get(0);
				System.out.println("Result for ParamILS: " + run.getRunResult() + ", " + run.getRuntime() + ", " + run.getRunLength() + ", " + run.getQuality() + ", " +run.getResultSeed());
			}
			
		} catch(ParameterException e)
		{
			OptionsToUsage.usage(UsageSectionGenerator.getUsageSections(args2));
			e.printStackTrace();

			
		}
		
		
		
	}

	public static class One {
		@Parameter(names="--a")
		public String a;
	}
	
	public static class Two {
		@Parameter(names="--b")
		public String b;
	}
	
	
}

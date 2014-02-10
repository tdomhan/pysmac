package ca.ubc.cs.beta.aclib.runconfig;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ec.util.MersenneTwister;

public class RunConfigHelper {

	public static RunConfig getRandomSingletonRunConfig()
	{
		return new RunConfig(new ProblemInstanceSeedPair(new ProblemInstance("Random"), (long) (Math.random()*100000)),124.0, ParamConfigurationSpace.getSingletonConfigurationSpace().getRandomConfiguration(new MersenneTwister()));
		
	}

}

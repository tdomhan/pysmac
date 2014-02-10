package ca.ubc.cs.beta.aclib.example.verifyscenario;



import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.returnvalues.ACLibReturnValues;
import ca.ubc.cs.beta.aclib.misc.string.SplitQuotedString;
import ca.ubc.cs.beta.aclib.misc.version.VersionTracker;
import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceOptions.TrainTestInstances;

public class VerifyScenarioExecutor {

	private static Logger log = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

		VerifyScenarioOptions opts = new VerifyScenarioOptions();

		
		try {
			//Get JCommander Object
			JCommander jcom;
			try {
				 jcom = JCommanderHelper.parseCheckingForHelpAndVersion(args, opts);
			} finally
			{
				opts.logOptions.initializeLogging();
				log = LoggerFactory.getLogger(	VerifyScenarioExecutor.class);
				VersionTracker.logVersions();
				
			}
			
			
			log.warn("This tool is current a BETA version and has some limitations. One limitation is that scenario files that specify non-scenario options which are permitted in SMAC will not be allowed here. Please report other discrepancies.");
			
			
			log.info("Found {} Scenarios to Validate", opts.scenarios.size());
			if(opts.checkInstances)
			{
				log.info("Will verify existance of instances on disk");
			}
			Set<String> searchDirectories = new LinkedHashSet<String>();
			//First entry should be experiment directory 
			//This is a stupid way to handle experiment directories so 
			//feel free to rewrite.
			searchDirectories.add(new File(opts.experimentDir).getAbsolutePath());
			searchDirectories.add(new File(".").getAbsolutePath());
			
			
			int maxLength = -1;
			for(String s : opts.scenarios)
			{
				File f = new File(s);
				String name = f.getName();
				maxLength = Math.max(maxLength, name.trim().length());
			}
			for(String s : opts.scenarios)
			{
				verifyScenario(s,new ArrayList<String>(searchDirectories), opts.checkInstances, opts.details,maxLength, opts.restoreScenarioArguments);
			}
			
			
			
		} catch(ParameterException e)
		{
			log.info("Error occured: {}", e.getMessage());
			log.debug("Exception",e);
			System.exit(ACLibReturnValues.PARAMETER_EXCEPTION);
		}
	}
	
	
	
	private static void verifyScenario(String s, List<String> searchDirectories, boolean checkInstances, boolean outputDetails,int maxLength, String restoreScenarioArgs) {
		
		
		File f = new File(s);
		String name = String.format("%-"+maxLength + "s",f.getName().trim());
		log.debug("Attempting to verify scenario file : {}", name);
		
		ScenarioOptions scenOpts;
		try {
			try {
				scenOpts = getScenarioOptions(s, restoreScenarioArgs);
			} catch(ParameterException e)
			{

				//Here we are manually checking if there is an invalid scenario key
				Properties p = new Properties();
				
				try
				{
					p.load(new FileReader(f));
				} catch(IOException e2)
				{
					throw e;
				}
				String reason = p.getProperty(ScenarioOptions.invalidScenarioKey);
				
				if(reason != null && reason.trim().length() > 0 )
				{
					log.info("Scenario {} is (probably) OK.   We cannot verify it, but it says: " + reason.trim() + "", name);	
					return;
					
				}
				throw e;
			}
			
		} catch(RuntimeException e)
		{
			log.error("Scenario {} failed verification due to problem parsing scenario file: {}", name,  e.getMessage());
			log.debug("Exception: ",e);
			return;
		}
		
		log.debug("Verifying PCS File: {}", scenOpts.algoExecOptions.paramFileDelegate.paramFile);
		double pcsLB;
		double pcsUB;
		try 
		{
			
			ParamConfigurationSpace configSpace  =scenOpts.algoExecOptions.paramFileDelegate.getParamConfigurationSpace(searchDirectories);
			pcsLB = configSpace.getLowerBoundOnSize();
			pcsUB = configSpace.getUpperBoundOnSize();
		} catch(RuntimeException e)
		{
			log.error("Scenario {} failed verification due to problem parsing PCS file {}. error was: {} ",name, scenOpts.algoExecOptions.paramFileDelegate.paramFile, e.getMessage());
			log.debug("Exception ",e);
			return;
		}
		
		log.debug("Verifying Execution Directory: {}", scenOpts.algoExecOptions.algoExecDir);
		
		File execDir = null;
		try
		{
			AlgorithmExecutionConfig execConfig = scenOpts.algoExecOptions.getAlgorithmExecutionConfig(searchDirectories, true);
			
			execDir = new File(execConfig.getAlgorithmExecutionDirectory());
			
			if(!execDir.exists())
			{
				if(searchDirectories.size() > 0)
				{
					execDir = new File(searchDirectories.get(0) + File.separator + execDir);
					
					if(!execDir.exists())
					{
						throw new RuntimeException("Couldn't find execution directory " + execDir);
					}
				} else if(!execDir.isDirectory())
				{
					throw new RuntimeException("Execution Directory specified isn't a directory");
				}
			} else if(!execDir.isDirectory())
			{
				throw new RuntimeException("Execution Directory specified isn't a directory: " + execDir);
				
			} 
			
			
		} catch(RuntimeException e)
		{
			log.error("Scenario {} failed verification due to problem with execdir field, error was: {} ",name, e.getMessage());
			log.debug("Exception ",e);
			return;
		}
		
		
		
		//Check if the output directory has changed 
		if(!scenOpts.outputDirectory.equals(new ScenarioOptions().outputDirectory))
		{
			log.warn("Scenario {} verification detected that the output directory was set in the scenario, this is discouraged and will be removed in future: outdir: {}", name, scenOpts.outputDirectory);
		}
		
		
		log.debug("Verifying Problem Instances: {}", scenOpts.instanceOptions.instanceFile);
		
		int instances;
		
		int features = -1;
		boolean noFeatures = false;
		
		try {
			TrainTestInstances tti = scenOpts.getTrainingAndTestProblemInstances(searchDirectories.get(0), 0, 0, true, false, false, false);
			instances = tti.getTrainingInstances().getInstances().size();
	
			if((tti.getTrainingInstances().getInstances().size() > 1) && (scenOpts.instanceOptions.instanceFeatureFile == null || scenOpts.instanceOptions.instanceFeatureFile.trim().equals("")))
			{
				log.warn("Scenario {} verification detected that no feature file is present and there is more than one instance, features are HIGHLY recommended", name);
				noFeatures = true;
			}
			
			if(checkInstances)
			{
				int failedInstances = 0;
				for(ProblemInstance pi : tti.getTrainingInstances().getInstances())
				{
					
					
					if((new File(pi.getInstanceName())).isAbsolute())
					{
						if(!(new File(pi.getInstanceName()).canRead()))
						{
							failedInstances++;
							log.error("Scenario {} verification can't find or read the following instance on disk: {}", name, pi.getInstanceName());
						}
					} else
					{
						if(!(new File(execDir.getAbsolutePath() + File.separator + pi.getInstanceName()).canRead()))
						{
							failedInstances++;
							log.error("Scenario {} verification can't find or read the following instance on disk: {}", name, pi.getInstanceName());
						}
						
					}
				}
				if(failedInstances > 0)
				{
					log.error("Scenario {} verification failed and because it couldn't read {} of {} instances ", name, failedInstances, instances);
					return;
				}
			}

			boolean absoluteDetected = false;
			for(ProblemInstance pi : tti.getTrainingInstances().getInstances())
			{
				
				if(new File(pi.getInstanceName()).isAbsolute())
				{
					if(absoluteDetected == false)
					{
						
						log.debug("Absolute instance name detected example: {} ", pi.getInstanceName());
						absoluteDetected = true;
					}
				}
				
			}
			
			if(absoluteDetected)
			{
				log.warn("Scenario {} verification detected absolute instance names, this can make the scenario non-portable and brittle and is discouraged",name);
			}  
					
					
			if(!noFeatures)
			{	
				for(ProblemInstance pi: tti.getTrainingInstances().getInstances())
				{
					features = Math.max(features,  pi.getFeaturesDouble().length);
					
				}
				
				if(features == 0)
				{
					log.warn("Scenario {} has a feature file but we detected NO features (probably the wrong feature file)", name);
				}
			}
		
		} catch(RuntimeException e)
		{
			log.error("Scenario {} verification failed due to problem reading instances: {}",name, e.getMessage());
			log.debug("Exception", e);
			return;
		}	
		 catch (IOException e) {
			log.error("Verify Scenario {} failed due to problem reading instances, error was: {}", name, e);
			return;
		}
		
		if(outputDetails)
		{
			String info = String.format("Scenario %s is OK  (%5d instances with %4d features, %5.1f cutoff (s), %7d tunertime (s),  PCS Size is in: [%11.7g,%11.7g])", name, instances,Math.max(0,features), scenOpts.algoExecOptions.cutoffTime, scenOpts.limitOptions.tunerTimeout,  pcsLB, pcsUB);
			
			log.info(info);
		} else
		{
			log.info("Scenario {} is OK", name);
		}
		return;
	}
	
	private static ScenarioOptions getScenarioOptions(String file, String restoreScenarioArgs)
	{
		
		ArrayList<String> argL = new ArrayList<String>();
		
		argL.add("--scenario-file");
		argL.add(file);
		
		argL.addAll(Arrays.asList(SplitQuotedString.splitQuotedString(restoreScenarioArgs)));
		
		
		
		
		ScenarioOptions scenOptions = new ScenarioOptions();
		
		JCommander jcom = JCommanderHelper.parseCheckingForHelpAndVersion(argL.toArray(new String[0]), scenOptions);
		
		return scenOptions;
	}

}

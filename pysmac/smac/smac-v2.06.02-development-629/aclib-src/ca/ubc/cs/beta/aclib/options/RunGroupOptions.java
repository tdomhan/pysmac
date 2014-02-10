package ca.ubc.cs.beta.aclib.options;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.misc.returnvalues.ACLibReturnValues;

@UsageTextField(hiddenSection = true)
public class RunGroupOptions extends AbstractOptions {

	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.BASIC)
	@Parameter(names={"--rungroup","--rungroup-name","--runGroupName"}, description="name of subfolder of outputdir to save all the output files of this run to")
	public String runGroupName; 
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--print-rungroup-replacement-and-exit", description="print all the possible replacements in the rungroup and then exit")
	public boolean runGroupExit;
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--rungroup-char","--runGroupReplacement"}, description="Character (potentially regex see source) to use as the start of a replacement in the runGroupName", hidden=true)
	public String replacementChar = "%";
	
	public RunGroupOptions(String defaultRunGroupName)
	{
		this.runGroupName = defaultRunGroupName;
	}
	
	
	public String getRunGroupName(Collection<AbstractOptions> opts)
	{
		Map<String, String> replacementMap = new TreeMap<String,String>();
		
		for(AbstractOptions opt : opts)
		{
			
			opt.populateOptionsMap(replacementMap);
		}
		
		replacementMap.putAll(System.getenv());
		
		replacementMap.put("SCENARIO_NAME", "NoScenarioFile");
		replacementMap.put("SCENARIO_FILE", "NoScenarioFile");
		if(replacementMap.get("scenarioFile") != null)
		{
			File f = new File(replacementMap.get("scenarioFile"));
			
			if(f != null)
			{
				
				if(f.getName().lastIndexOf(".") >= 0)
				{
					replacementMap.put("SCENARIO_NAME", f.getName().substring(0,f.getName().lastIndexOf(".")));
				} else
				{
					replacementMap.put("SCENARIO_NAME", f.getName());
				}
				
				replacementMap.put("SCENARIO_FILE", f.getName());
			}
		}
		replacementMap.put("DATETIME", (new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss-SSS").format(new Date())));
		replacementMap.put("DATE", (new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
		return getRunGroup(replacementMap);
	}
	
	/**
	 * Converts the supplied runGroup into a string given the replacements
	 * 
	 * 
	 * @param replacementMap replacement map
	 * @return
	 */
	private String getRunGroup(Map<String, String> replacementMap)
	{

		
		//Sort the options case insensitively
		Map<String, String> sortedReplacementMap = new TreeMap<String,String>(new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2) {
					return o1.toUpperCase().compareTo(o2.toUpperCase());
			}
		}
		);
		sortedReplacementMap.putAll(replacementMap);
		
		
		
		if(runGroupExit)
		{
			System.out.println("Replacements printed in order they will be replaced");
			for(Entry<String, String> ent : sortedReplacementMap.entrySet())
			{
				System.out.println("Replacement : " + ent.getKey() + " ===> " + ent.getValue());
			}
			
		}

		//Sort options in decending order of length
		//We will replace long strings first
		Map<String, String> orderedReplacementMap = new TreeMap<String, String>(new Comparator<String>()
				{

					@Override
					public int compare(String o1, String o2) {
						if(o1.length() != o2.length())
						{
							return o2.length() - o1.length();
						}else
						{
							return o1.compareTo(o2);
						}
					}
				});

		
		orderedReplacementMap.putAll(replacementMap);

		String line = this.runGroupName;
	
		for(Entry<String, String> ent : orderedReplacementMap.entrySet())
		{
			line = line.replaceAll(this.replacementChar + ent.getKey(), ent.getValue());
		}
		
		File dir;
		try {
		 dir = createTempDirectory();
		} catch (IOException e) {
			throw new IllegalStateException("Could not create temporary directory");
		}
		
		File f = new File(dir.getAbsolutePath() + File.separator + line);
		
		dir.deleteOnExit();
		try {
			if(!(f.exists() || f.mkdirs()))
			{
				throw new ParameterException("Could not create directory based on runGroupName original value " + this.runGroupName + " ===> " + line + ". Please check that the substitution obeys file system rules");
			}
			
		} catch(ParameterException e)
		{
			if(runGroupExit)
			{
				System.out.println(e.getMessage());
				System.out.println("Final substitution: " + line);
				System.exit(ACLibReturnValues.PARAMETER_EXCEPTION);
			} else
			{
				throw e;
			}
		}
		
		if(runGroupExit)
		{
			System.out.println("Final substitution: " + line);
			System.exit(ACLibReturnValues.SUCCESS);
		}
		
	
		return line;
			
			

			
	}

	public static File createTempDirectory() throws IOException
	{
	    final File temp;

	    temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

	    if(!(temp.delete()))
	    {
	        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
	    }

	    if(!(temp.mkdir()))
	    {
	        throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
	    }

	    return (temp);
	}
	
	
	public String getFailbackRunGroup() {

		return this.runGroupName.replaceAll("%", "");
	}
}

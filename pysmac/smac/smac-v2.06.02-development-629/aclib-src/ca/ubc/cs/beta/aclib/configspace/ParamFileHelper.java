package ca.ubc.cs.beta.aclib.configspace;

import java.io.File;
import java.io.StringReader;

/**
 * Contains Factory Methods for getting ParamConfigurationSpaces
 * 
 * 
 */
public final class ParamFileHelper {

	/**
	 * Returns a ParamConfigurationSpace via the filename and seeded with seed
	 * 
	 * @param 	filename				 string for the filename
	 * @return	ParamConfigurationSpace  the configuration space
	 * 
	 */
	public static ParamConfigurationSpace getParamFileParser(String filename)
	{	if(filename.equals(ParamConfigurationSpace.SINGLETON_ABSOLUTE_NAME))
		{
			return ParamConfigurationSpace.getSingletonConfigurationSpace();
		} else if(filename.equals(ParamConfigurationSpace.NULL_ABSOLUTE_NAME))
		{
			return ParamConfigurationSpace.getNullConfigurationSpace();
		} else
		{
			return getParamFileParser(new File(filename));
		}
	}

	/**
	 * Returns a ParamConfigurationSpace via the filename and seeded with seed
	 * 
	 * @param file  					file with the param arguments
	 * @return ParamConfigurationSpace instance
	 */
	public static ParamConfigurationSpace getParamFileParser(File file)
	{
		return new ParamConfigurationSpace(file);
	}

	public static ParamConfigurationSpace getParamFileFromString(String string) {
		return new ParamConfigurationSpace(new StringReader(string));
	}
	
	//Non-initializable
	private ParamFileHelper()
	{
		
	}
}

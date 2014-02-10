package ca.ubc.cs.beta.aclib.configspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import net.jcip.annotations.Immutable;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.misc.java.io.FileReaderNoException.FileReaderNoExceptionThrown;
import ec.util.MersenneTwisterFast;

enum LineType
{
	CATEGORICAL,
	CONTINUOUS,
	CONDITIONAL,
	FORBIDDEN,
	OTHER
}



/**
 * This parses the parameter specification file into a nice OO view for us, and stores the data in relevant data structures.
 * <p>
 * The aim of this file is more readability than simplicity as such some of the data structures like paramNames are redundant.
 * <p>
 * This object is effectively immutable and this should not change under any circumstances (for thread safety reasons)
 * <p>
 * <b>Historical Note:</b> This class originally was very Collection heavy, however as the ParamConfiguration objects 
 * are all backed by arrays it really would make more sense for this it to be backed by more arrays.
 * Some of the data structures in here are redundant. This class could use a clean up, but has far reaching consequences.
 * 
 * @author seramage
 */
@Immutable
public class ParamConfigurationSpace implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9137375058810057209L;

	/**
	 * Stores a list of values for each parameter name
	 */
	private final Map<String,List<String>> values = new HashMap<String, List<String>>();
	
	/**
	 * Stores the default value for each parameter name
	 */
	private final Map<String,String> defaultValues = new HashMap<String, String>();
	
	/**
	 * For each parameter name stores the name of the parameters and the required values of those parameters, for this parameter to be active. 
	 */
	private final Map<String, Map<String, List<String>>> dependentValues = new HashMap<String, Map<String,List<String>>>();
	
	/**
	 * For each parameter stores a boolean for whether or not the value is continuous
	 */
	private final Map<String, Boolean> isContinuous = new HashMap<String, Boolean>();
	
	/**
	 * Stores a list of parameter names
	 * @deprecated the paramKeyIndexMap stores the same information, and will iterate the same way
	 * with a slightly more cumbersome interface unfortunately.
	 */
	@Deprecated
	private final List<String> paramNames = new LinkedList<String>();
	
	/**
	 * For each Categorical Parameter, maps the value of input, into the value the random forest expects
	 */
	private final Map<String, Map<String, Integer>> categoricalValueMap = new HashMap<String, Map<String, Integer>>();
	
	
	/**
	 * Used as the basis of hashCode calculations
	 * Two ParamFileParsers are equal iff the file they reference are equal.
	 */
	private final String absoluteFileName;
	
	
	private final Map<String, NormalizedRange> contNormalizedRanges = new HashMap<String, NormalizedRange>();
	
	/**
	 * Stores a mapping of Parameter Names to index into the representative arrays.
	 * The iteration order of this is guaranteed to be authorative (that is you will
	 * always get the first index first, then the second, then the third)...
	 */
	private final Map<String, Integer> paramKeyIndexMap = new LinkedHashMap<String, Integer>();
	private final boolean[] parameterDomainContinuous;
	private final int[] categoricalSize;

	
	private final List<int[][]> forbiddenParameterValuesList = new ArrayList<int[][]>();
	/**
	 * Value to store in the categoricalSize array for continuous parameters
	 */
	public static final int INVALID_CATEGORICAL_SIZE = 0;
	
	/**
	 * Number of parameters 
	 */
	private final int numberOfParameters;
	
	/**
	 * For each parameter (in authorative order) stores the indexes of the parent parameters
	 */	
	private final int[][] condParents;

	
	private final int[][][] condParentVals;


	/**
	 * Flag variable that controls whether there exists a real PCS file
	 * NOTE: This field should not be changed after being set.
	 */
	private boolean hasRealParameterFile = true;
	
	/**
	 * Stores forbidden lines for later parsing
	 */
	private final List<String> forbiddenLines = new ArrayList<String>();

	/**
	 * Gets the default configuration
	 */
	private final double[] defaultConfigurationValueArray;
	
	/**
	 * For each parameter stores the subspace value we are looking at.
	 */
	final double[] searchSubspaceValues;
	
	/**
	 * Stores whether we should only be looking at the subspace for the parameter.
	 */
	final boolean[]  searchSubspaceActive;
	
	
	/**
	 * Stores the names of parameters in order
	 * Synchronized because of lazy loading, this should be done better later
	 * 
	 * The authorative order is in fact something that should be specified in the constructor but for now sorting seems to work
	 * 
	 * 
	 */
	private final List<String> authorativeParameterNameOrder;

	/**
	 * Array representation of the previous value to speed up searching
	 */
	private final String[] authorativeParameterOrderArray;
	
	/**
	 * Array representation of the ranges by index 
	 */
	private final NormalizedRange[] normalizedRangesByIndex;

	/**
	 * Creates a Param Configuration Space from the given file, no random object
	 * @param filename string storing the filename to parse
	 */
	public ParamConfigurationSpace(String filename)
	{
		this(new File(filename));
	}
	
	
	/**
	 * 
	 * @param reader
	 * @param searchSubspace
	 */
	public ParamConfigurationSpace(Reader reader, Map<String, String> searchSubspace)
	{
		this(reader, "ReaderOnly-"+System.currentTimeMillis() +"-" +(int) (Math.random() * 10000000.0), searchSubspace);
		hasRealParameterFile = false;
	}
	
	
	/**
	 * Creates a Param Configuration Space from the given file, no random object
	 * @param reader r
	 */
	public ParamConfigurationSpace(Reader reader)
	{
 		this(reader, "ReaderOnly-"+System.currentTimeMillis() +"-" +(int) (Math.random() * 10000000.0));
		hasRealParameterFile = false;
	}
	
	
	/**
	 * Creates a Param Configuration Space from the given file
	 * @param file	 File containing the configuration space
	 */
	public ParamConfigurationSpace(File file)
	{
		this(new FileReaderNoExceptionThrown(file), file.getAbsolutePath());
	}
	
	
	
	/**
	 * Creates a Param Configuration Space from the given reader
	 * @param file 			    A reader object that will allow us to read the configuration space
	 * @param absoluteFileName  A file name of the object (a unique string used for equality)
	 */
	@SuppressWarnings("unchecked")
	public ParamConfigurationSpace(Reader file, String absoluteFileName)
	{
		this(file, absoluteFileName, Collections.EMPTY_MAP);
	}
	
	/**
	 * Creates a Param Configuration Space from the given reader
	 * @param file 			    				A reader object that will allow us to read the configuration space
	 * @param absoluteFileName  				A file name of the object (a unique string used for equality)
	 * @param searchSubspace					A map that controls which parameters should be used to generate a subspace
	 */
	public ParamConfigurationSpace(Reader file, String absoluteFileName, Map<String, String> searchSubspace)
	{
		
		/*
		 * Parse File and create configuration space
		 */
		this.absoluteFileName = absoluteFileName;
		
		if((absoluteFileName == null) || (absoluteFileName.trim().length() == 0))
		{
			throw new IllegalArgumentException("Absolute File Name must be non-empty:" + absoluteFileName);
		}
		try {
			BufferedReader inputData = null;
			
			try{
			
			inputData = new BufferedReader(file);
			String line;
			while((line = inputData.readLine()) != null)
			{ try {
				parseLine(line);
				} catch(RuntimeException e)
				{
					System.err.println("Error occured parsing: " + line);
					throw e;
				}
			}
			} finally
			{
				Collections.sort(paramNames);
				if (inputData != null)
				{
					inputData.close();
				}
			}
			
			
		} catch (FileNotFoundException e) {

			throw new IllegalStateException(e);
		} catch (IOException e) {
			
			
			throw new IllegalStateException(e);
		}
		
		
		
		/*
		 * Create data structures necessary for ParamConfiguration objects
		 * 
		 * Alot of this is redundant with what's above as this code was added
		 * as part of refactoring. This class could use a clean up
		 * 
		 * This gets the data into a format convenient for Random Forests.
		 * 
		 */		
		List<String> paramOrder = new ArrayList<String>(paramNames.size());
		paramOrder.addAll(paramNames);
		Collections.sort(paramOrder);
		
		this.authorativeParameterNameOrder = Collections.unmodifiableList(paramOrder);
		
		this.authorativeParameterOrderArray = new String[this.authorativeParameterNameOrder.size()];
		this.normalizedRangesByIndex = new NormalizedRange[this.authorativeParameterNameOrder.size()];
		for(int i=0; i < authorativeParameterNameOrder.size(); i++)
		{
			this.authorativeParameterOrderArray[i] = authorativeParameterNameOrder.get(i);
			this.normalizedRangesByIndex[i] = this.contNormalizedRanges.get(this.authorativeParameterOrderArray[i]);
		}
		
		
		
		
		
		  //TODO: Expand on these comments
		  parameterDomainContinuous = new boolean[paramNames.size()];
		  categoricalSize = new int[paramNames.size()];
		  this.numberOfParameters = paramNames.size();
		  int i=0;
		  for(String paramName : getParameterNamesInAuthorativeOrder())
		  {  
			paramKeyIndexMap.put(paramName,i);
			
			parameterDomainContinuous[i] = getContinuousMap().get(paramName);
			if(parameterDomainContinuous[i] == false)
			{
				categoricalSize[i] = getValuesMap().get(paramName).size();
			} else
			{
				categoricalSize[i] = INVALID_CATEGORICAL_SIZE;
			}
			
			i++;
		  }
		  
		  
		  
		  condParents = new int[numberOfParameters][];
		  condParentVals = new int[numberOfParameters][][];
		  
			Map<String, Map<String, List<String>>> depValueMap = getDependentValuesMap();
			
			for(String key : depValueMap.keySet())
			{
				if(!this.paramKeyIndexMap.keySet().contains(key))
				{
					throw new IllegalArgumentException("Illegal independent value ("+ key+") specified on conditional line.");
				}
			}
			for(i=numberOfParameters; i < numberOfParameters;i++)
			{
				condParents[i] = new int[0];
				condParentVals[i] = new int[0][];
			}
			
			
			for( i=0; i < numberOfParameters; i++)
			{
				
				String key = getParameterNames().get(i);
				
				//System.out.println("key => " + key);

				Map<String, List<String>> depValues = depValueMap.get(key); 
				if((depValues == null) || (depValues.size() == 0))
				{
					condParents[i] = new int[0];
					condParentVals[i] = new int[0][];
					continue;
				}
				
				condParents[i] = new int[depValues.size()];
				condParentVals[i] = new int[depValues.size()][];
				
				int j=0;
				for(Entry<String, List<String>> e : depValues.entrySet())
				{
					
					if(paramKeyIndexMap.get(e.getKey())  == null)
					{
						throw new IllegalArgumentException("Illegal dependant parameter ("+ e.getKey()+ ") on conditional line in param file: "); 
					}
					
					condParents[i][j] = paramKeyIndexMap.get(e.getKey()) ;
					condParentVals[i][j] = new int[e.getValue().size()]; 
					for(int k=0; k < e.getValue().size(); k++)
					{

						String depValue = e.getValue().get(k);
						String depKey = e.getKey();
						
						if(isContinuous.get(depKey))
						{
							throw new IllegalArgumentException("Value depends upon continuous parameter, this is not supported: " + key + " depends on " + depKey + " values: " + depValue);
						}
						
						if(!getCategoricalValueMap().get(depKey).keySet().contains(depValue))
						{
							throw new IllegalArgumentException("Value depends upon a non-existant or invalid parameter value: " + key + " depends on " + depKey + " having invalid value: " + depValue);
						}
				
						condParentVals[i][j][k] = getCategoricalValueMap().get(e.getKey()).get(e.getValue().get(k));
						
						condParentVals[i][j][k]++;
						
					}
					j++;	
				}
				
			}

		  
			
			for(String forbiddenLine : forbiddenLines )
			{
				parseForbiddenLine(forbiddenLine);
				
			}
			forbiddenLines.clear();
			
		this.defaultConfigurationValueArray = _getDefaultConfiguration().toValueArray();
		
		
	
		
		/*
		 * This will basically test that 
		 * the default configuration is actually valid
		 * This is a fail fast test in case the parameter values are invalid
		 */
		if(this.getDefaultConfiguration().isForbiddenParamConfiguration())
		{
			throw new IllegalArgumentException("Default parameter setting cannot be a forbidden parameter setting");
		}
		
		this.searchSubspaceValues = new double[this.defaultConfigurationValueArray.length];
		this.searchSubspaceActive = new boolean[this.defaultConfigurationValueArray.length];
		
		for(Entry<String, String> subspaceProfile : searchSubspace.entrySet())
		{
			String param = subspaceProfile.getKey();
			String value = subspaceProfile.getValue();
			
			if(value.equals("<DEFAULT>"))
			{
				value = this.getDefaultConfiguration().get(param);
			}
			
			int index = this.paramKeyIndexMap.get(param);
			
			
			setValueInArray(this.searchSubspaceValues, param, value);
			this.searchSubspaceActive[index] = true;
		}
		
		
		
	}
	
	/**
	 * Returns the filename that we parsed in absolute form
	 * @return string representating the filename of the param file
	 */
	public String getParamFileName()
	{
		return absoluteFileName;
	}
	
	/**
	 * Parses a line from the param file, populating the relevant data structures.
	 * @param line line of the param file
	 */
	private void parseLine(String line)
	{
		//Default to a LineType of other 
		LineType type = LineType.OTHER;
		
		//Removes Comment
		int commentStart = line.indexOf("#");
		line = line.trim();
		if (commentStart >= 0)
		{
			line = line.substring(0, commentStart).trim();
		}
		
		
		//We are done if we trim away the rest of the line
		if (line.length() == 0)
		{
			return;
		}
	
		
		/** 
		 * Perhaps not the most robust but the logic here is as follows
		 * if we see a "|" it's a conditional line
		 * otherwise we expect to see a "[" (if we don't we have no idea) what it is. 
		 * If we see two (Or more) "[" then allegedly it's continous (one specifies the range the other the default).
		 * Otherwise we must be categorical
		 * 
		 * This is hardly robust and easily tricked.
		 */
		if (line.indexOf("|") >= 0)
		{
			type = LineType.CONDITIONAL;
		} else if(line.trim().substring(0, 1).equals("{"))
		{
			type = LineType.FORBIDDEN;
		} else if(line.indexOf("[") < 0)
		{
			type = LineType.OTHER;
			if(line.trim().equals("Conditionals:")) return;
			if(line.trim().equals("Forbidden:")) return;
			
			throw new IllegalArgumentException("Cannot parse the following line:" + line);
		} else if (line.indexOf("[") != line.lastIndexOf("["))
		{
			type = LineType.CONTINUOUS;
		} else if(line.trim().indexOf("{") > 1 && line.trim().indexOf("}") > 1 )
		{
			type = LineType.CATEGORICAL;
		} else
		{
			throw new IllegalArgumentException("Syntax error parsing line " + line + " probably malformed");
		}
		
		switch(type)
		{
			case CONDITIONAL:
				parseConditionalLine(line);
				break;
			case CATEGORICAL:
				parseCategoricalLine(line);
				break;
			case CONTINUOUS:
				parseContinuousLine(line);
				break;
			case FORBIDDEN:
				forbiddenLines.add(line);
				break;
			default:
				throw new IllegalStateException("Not sure how I can be parsing some other type, ");
		}
		
		
	}
	
	private void parseForbiddenLine(String line) {
		
		String originalLine = line;
		
		if(line.trim().indexOf("{",1) != -1) throw new IllegalArgumentException("Line specifying forbidden parameters contained more than one { in line: " + originalLine);
		
		line = line.replace("{", "");
		
		
		if(line.trim().indexOf("}") == -1) throw new IllegalArgumentException("Line specifying forbidden parameters contained no closing brace \"}\" in line: " + originalLine);
		
		line = line.replaceFirst("}","");
		
		if(line.trim().indexOf("}") != -1) throw new IllegalArgumentException("Line specifying forbidden parameters contained multiple closing braces \"}\" in line: " + originalLine);
		
		
		
		
		String[] nameValuePairs = line.split(",");
		
		List<int[]> forbiddenIndexValuePairs = new ArrayList<int[]>();
				
		for(String nameValuePair : nameValuePairs)
		{
			String[] nvPairArr = nameValuePair.split("=");
			if(nvPairArr.length != 2)
			{
				throw new IllegalArgumentException("Line specifying forbidden parameters contained an name value pair that could not be parsed: "+ Arrays.toString(nvPairArr) + " in line: " + originalLine);
			}
			
			String name = nvPairArr[0].trim();
			Integer indexIntoValueArrays = paramKeyIndexMap.get(name);
			
			if(indexIntoValueArrays == null)
			{
				throw new IllegalArgumentException("Unknown parameter " + name + " in line: " + originalLine);
			}
			
			String value = nvPairArr[1].trim();
			
			if(isContinuous.get(name))
			{
				throw new IllegalArgumentException("Forbidden Parameter Declarations can only exclude combinations of categorical parameters " + name + " is continuous; in line: " + line );
			}
			
			Integer valueIndex = categoricalValueMap.get(name).get(value);
			
			if(valueIndex == null)
			{
				throw new IllegalArgumentException("Invalid parameter value " + value + " for parameter " + name + " in line: " + line);
				
			}
			
			
			
			
			int[] nvPairArrayForm = new int[2];
			nvPairArrayForm[0] = indexIntoValueArrays;
			nvPairArrayForm[1] = valueIndex; 
			
			forbiddenIndexValuePairs.add(nvPairArrayForm);
		}
		
		
		forbiddenParameterValuesList.add(forbiddenIndexValuePairs.toArray(new int[0][0]));
		
	}
	/**
	 * Continuous Lines consist of:
	 *   
	 * <name><w*>[<minValue>,<maxValue>]<w*>[<default>]<*w><i?><l?>#Comment
	 * where:
	 * <name> - name of parameter.
	 * <minValue> - minimum Value in Range
	 * <maxValue> - maximum Value in Range.
	 * <default> - default value enclosed in braces.
	 * <w*> - zero or more whitespace characters
	 * <i?> - An optional i character that specifies whether or not only integral values are permitted
	 * <l?> - An optional l character that specifies if the domain should be considered logarithmic (for sampling purposes).
	 * 
	 * @param line
	 */
	private void parseContinuousLine(String line) 
	{
		
		String name = getName(line);
		int firstBracket = line.indexOf("[");
		int secondBracket = line.indexOf("]");
		String domainValues = line.substring(firstBracket+1, secondBracket);
		String[] contValues  = domainValues.split(",");
		if(contValues.length != 2)
		{
			throw new IllegalArgumentException ("Expected two parameter values (or one comma between the first brackets) from line \""+ line + "\" but received " + contValues.length);
		}
		
		double min = Double.valueOf(contValues[0]);
		double max = Double.valueOf(contValues[1]);

		String defaultValue = getDefault(secondBracket, line);
		
		paramNames.add(name);
		isContinuous.put(name, Boolean.TRUE);
		values.put(name, Collections.<String> emptyList());
		
		this.defaultValues.put(name, defaultValue);
		
		
		
		
		
		//This gets the rest of the line after the defaultValue
		String lineRemaining = line.substring(line.indexOf("]",secondBracket+1)+1);
		
		
		
		boolean logScale = ((lineRemaining.length() > 0) && (lineRemaining.trim().contains("l")));
		lineRemaining = lineRemaining.replaceFirst("l", "").trim();
		
		boolean intValuesOnly = ((lineRemaining.length() > 0) && (lineRemaining.trim().contains("i")));
		
		if(intValuesOnly)
		{
			try {
			
			if(!isIntegerDouble(Double.valueOf(contValues[0]))) throw new IllegalArgumentException("This parameter is marked as integer, only integer values are permitted for the bounds and default on line:" + line); 
			if(!isIntegerDouble(Double.valueOf(contValues[1]))) throw new IllegalArgumentException("This parameter is marked as integer, only integer values are permitted for the bounds and default on line:" + line);
			if(!isIntegerDouble(Double.valueOf(defaultValue))) throw new IllegalArgumentException("This parameter is marked as integer, only integer values are permitted for the bounds and default on line:" + line);
			} catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("This parameter is marked as integer, only integer values are permitted for the bounds and default on line:" + line);
			}
		}
		
		
		lineRemaining = lineRemaining.replaceFirst("i", "").trim();		
		
		if(lineRemaining.trim().length() != 0)
		{
			throw new IllegalArgumentException("Unknown or duplicate modifier(s): " + lineRemaining + " in line: " + line);
		}
			
			
		try {
			contNormalizedRanges.put(name, new NormalizedRange(min, max, logScale, intValuesOnly));
		} catch(IllegalArgumentException e)
		{
			throw new IllegalArgumentException(e.getMessage() + "; error occured while parsing line: " +line);
		}

		
		//Check for gibberish between ] [  
		String subString = line.substring(line.indexOf("]") +1, line.indexOf("[", line.indexOf("]"))).trim();
		
		
		if(subString.length() > 0)
		{
			throw new IllegalArgumentException("Invalid Characters detected between domain and default value on line : " + line);
		}
		
		
	}

	/**
	 * Categorical Lines consist of:
	 *   
	 * <name><w*>{<values>}<w*>[<default>]<*w>#Comment
	 * where:
	 * <name> - name of parameter.
	 * <values> - comma seperated list of values (i.e. a,b,c,d...,z)
	 * <default> - default value enclosed in braces.
	 * <w*> - zero or more whitespace characters
	 * 
	 * 
	 * @param line
	 */
	private void parseCategoricalLine(String line) 
	{
		String name = getName(line);
		List<String> paramValues = getValues(line);
		String defaultValue = getDefault(0,line);
		
		paramNames.add(name);
		values.put(name, paramValues);
		
		//LinkedHashMap just makes it easier to debug
		//Map is all that is necessary
		Map<String, Integer> valueMap = new LinkedHashMap<String, Integer>();
		
		int lastIndexOf = line.lastIndexOf("]");
		
		
	
		
		int i=0;
		for(String value : paramValues)
		{
			valueMap.put(value, i);
			i++;
			
		}
		categoricalValueMap.put(name, valueMap);
		
		defaultValues.put(name, defaultValue);
		
		isContinuous.put(name,Boolean.FALSE);
		
		String remaining = line.substring(lastIndexOf+1).trim();
		
		if(remaining.contains("i")) {
			throw new IllegalArgumentException("Cannot set categorical value as having integral only values on line " +  line );
		}
		
		if(remaining.contains("l")) {
			throw new IllegalArgumentException("Cannot set categorical value as being selected with a log distribution" + line );
		}
		
		if(remaining.length() > 0)
		{
			throw new IllegalArgumentException("Extra flags set for categorical value (flags are not permitted on categorical values) on line : " + line);
		}
	}
	
	
	/**
	 * Conditional Lines consist of:
	 * <name1><w*>|<name2><w+><op><w+>{<values>} #Comment
	 * <name1> - dependent parameter name
	 * <name2> - the independent parameter name
	 * <op> - probably only in at this time.
	 * <w*> - zero or more whitespace characters.
	 * <w+> - one or more whitespace characters
	 * <values> - values 
	 * 
	 * Note: <op> only supports in at this time. (and the public interface only assumes in).
	 * @param line
	 */
	private void parseConditionalLine(String line) {
		String lineToParse = line;
		
		String name1 = getName(line);
		
		String name2 = getName(line.substring(line.indexOf("|") + 1));
		//System.out.println(name1 + " depends on " + name2);
		
		
		if(name1.equals(name2))
		{
			throw new IllegalArgumentException("Parameter " + name1 + " cannot be conditional on itself in line: " + line);
		}
		lineToParse = lineToParse.replaceFirst(name1,"");
		lineToParse = lineToParse.replaceFirst("|", "");
		lineToParse = lineToParse.replaceFirst(name2,"");
		lineToParse = lineToParse.trim();
		
		
				
		if (lineToParse.indexOf(" in ") < 0)
		{
			throw new IllegalStateException("Unknown or missing operator in line: " + line);
		}
		List<String> condValues = getValues(line);
		
		Map<String, List<String>> dependencies = dependentValues.get(name1);
		if( dependencies == null)
		{
			dependencies = new HashMap<String,List<String>>();
			dependentValues.put(name1, dependencies );
		}
	
		//If multiple lines appear for more than one clause I overwrite the previous value
		if (dependencies.get(name2) != null)
		{
			
			throw new IllegalArgumentException("Parameter " + name1 + " already has a previous dependency for " + name2 + " values {" + dependencies.get(name2).toString() + "}. Parameter dependencies respecified in line: " + line);
		}
			
			
		dependencies.put(name2, condValues);
		
		
	}
	
	
	/**
	 * Returns the name assuming it starts at the beginning of a line.
	 * @param line
	 * @return
	 */
	private String getName(String line)
	{
		return getName(line,0);
	}
	
	/**
	 * Returns the string from offset until the first " ", "|", "{" 
	 * @param offset
	 * @return
	 */
	private String getName(String line, int offset)
	{
		line = line.trim();
		/**
		 * We are looking for the stuff before the first " ", "|", or "{"
		 */
		int firstSpace = line.indexOf(" ");
		int firstPipe = line.indexOf("|");
		int firstCurly = line.indexOf("{");
		int firstSquare = line.indexOf("[");
		
		if (firstSpace == -1) firstSpace = Integer.MAX_VALUE;
		if (firstPipe == -1) firstPipe = Integer.MAX_VALUE;
		if (firstCurly == -1) firstCurly = Integer.MAX_VALUE;
		if (firstSquare == -1) firstSquare = Integer.MAX_VALUE;
		
		
		int nameBoundary = Math.min(firstSpace,firstPipe);
		
		nameBoundary = Math.min(nameBoundary, firstCurly);
		nameBoundary = Math.min(nameBoundary, firstSquare);
		String name =  line.substring(offset, nameBoundary).trim();
		if(name.length() == 0)
		{
			throw new IllegalArgumentException("Did Not Parse a Parameter Name in line: " + line);
		} else
		{
			return name;
		}
	}
	
	
	/**
	 *  Returns the default value (takes an offset from where to search)
	 * @param offset
	 * @param line
	 * @return
	 */
	private String getDefault(int offset, String line) {
		String defaultValue = line.substring(line.indexOf("[",offset+1)+1, line.indexOf("]",offset+1)).trim();
		if (defaultValue.length() == 0)
		{
			throw new IllegalArgumentException("Invalid Default Value specified in line: " + line);
		} else
		{
			return defaultValue;
		}
	}


	/**
	 * Gets the list of allowable values
	 * @param line
	 * @return
	 */
	private List<String> getValues(String line) {

		String oLine = line;
		int start = line.indexOf("{");
		int end = line.indexOf("}");
		
		line = line.substring(start+1,end);
		line = line.replace(',', '\n');
		
		BufferedReader r = new BufferedReader( new StringReader(line));
		
		List<String> strings = new LinkedList<String>();
		
		String value; 
		
		try {
			while ((value = r.readLine()) != null)
			{
				
					if (value.trim().length() == 0)
					{
						throw new IllegalArgumentException("Value cannot be empty (consist only of whitespace) in line: " + oLine);
					} else
					{
						strings.add(value.trim());
					}
				
			}
		} catch (IOException e) {

			System.err.println("Some random IOException occured?");
			e.printStackTrace();
			
			throw new IllegalStateException("An exception occured while reading values from (" + oLine + ") we mistakenly thought this would never happen, please contact developer", e);
		}
		
		Set<String> set = new HashSet<String>();
		set.addAll(strings);
		if(set.size() < strings.size())
		{
			throw new IllegalStateException("Duplicate Value detected in line: " +  oLine);
		}
		
		return strings;
	}


	

	public List<String> getParameterNames()
	{
		return Collections.unmodifiableList(paramNames);
	}
	
	public Map<String, Boolean> getContinuousMap()
	{
		return Collections.unmodifiableMap(isContinuous);
	}
	
	/**
	 * Returns a map of values for each parameter the list of string values
	 * 
	 * <b>WARN</b> While the map itself is immutable, messing with the internal data structures will corrupt the config space
	 * @return map of values
	 */
	public Map<String,List<String>> getValuesMap()
	{
		return Collections.unmodifiableMap(values);
	}
	
	
	public Map<String, String> getDefaultValuesMap()
	{
		return Collections.unmodifiableMap(defaultValues);
		
	}
	
	/**
	 * This R/O protection isn't robust
	 * 
	 */
	public Map<String, Map<String, List<String>>> getDependentValuesMap()
	{
		return Collections.unmodifiableMap(dependentValues);
	}
	
	/**
	 * This R/O protection isn't robust
	 */
	public Map<String, Map<String, Integer>> getCategoricalValueMap()
	{
		return Collections.unmodifiableMap(categoricalValueMap);
	}
	
	
	public Map<String, NormalizedRange> getNormalizedRangeMap() {
		return Collections.unmodifiableMap(contNormalizedRanges);
	}
	
	
	/**
	 * Returns a listing of parameter names in <b>authorative</b> order
	 * @return list of strings
	 */
	public List<String> getParameterNamesInAuthorativeOrder()
	{
		return authorativeParameterNameOrder;
	}
	
	/**
	 * Absolute File Name is the basis for the hashCode
	 */
	public int hashCode()
	{
		return absoluteFileName.hashCode();
	}
	
	
	/**
	 * Determines whether two configuration spaces are 'compatible'
	 * <br/>
	 * <b>Note:</b> Compatible here at the very least means the valueArrays should be interchangable, in future it may be stronger.
	 * 
	 * @return <code>true</code> if the configuration spaces are compatible
	 */
	public boolean isCompatible(ParamConfigurationSpace oSpace)
	{
		if(!Arrays.equals(this.categoricalSize, oSpace.categoricalSize))
		{
			return false;
		} else
		{
			return true;
		}
		
	}
	
	
	/**
	 * Two Entities are equal if they reference the same file
	 */
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if (o instanceof ParamConfigurationSpace)
		{
			ParamConfigurationSpace po = (ParamConfigurationSpace) o;
			if(po.absoluteFileName.equals(absoluteFileName))
			{
				if(Arrays.equals(searchSubspaceActive, po.searchSubspaceActive))
				{
					if(Arrays.equals(this.searchSubspaceValues, po.searchSubspaceValues))
					{
						return true;
					}
				}
			}
			
			
		}
		return false;
	}
	
	public String toString()
	{
		return "ParamFile:" + absoluteFileName;
	}
	
	/**
	 * Generates a random configuration given the supplied random object 
	 * @param random object we will use to generate the configuration 
 	 * @return a random member of the configuration space (each parameter (ignoring the subspace) is sampled uniformly at random, and rejected if it's forbidden).
	 */
	public ParamConfiguration getRandomConfiguration(Random random)
	{
		
		return this.getRandomConfiguration(random, false);
	}
	
	/**
	 * Returns a random instance for the configuration space
	 * @param random 	a random object we will use to generate the configurations
	 * @param allowForbiddenParameters  <code>true</code> if we can return parameters that are forbidden, <code>false</code> otherwise.
	 * @return	a random member of the configuration space (each parameter (ignoring the subspace) is sampled uniformly at random, and rejected if it's forbidden
	 */
	public ParamConfiguration getRandomConfiguration(Random random, boolean allowForbiddenParameters)
	{
		return getRandomConfiguration(new RandomAdapter(random), allowForbiddenParameters);
	}
	
	/**
	 * Generates a random configuration given the supplied random object 
	 * @param random a fast random object we will use to generate the configuration 
 	 * @return a random member of the configuration space (each parameter (ignoring the subspace) is sampled uniformly at random, and rejected if it's forbidden).
	 */
	public ParamConfiguration getRandomConfiguration(MersenneTwisterFast random)
	{
		
		return this.getRandomConfiguration(new RandomAdapter(random), false);
	}
	
	/**
	 * Returns a random instance for the configuration space
	 * @param random 	a fast random object we will use to generate the configurations
	 * @param allowForbiddenParameters  <code>true</code> if we can return parameters that are forbidden, <code>false</code> otherwise.
	 * @return	a random member of the configuration space (each parameter (ignoring the subspace) is sampled uniformly at random, and rejected if it's forbidden
	 */
	public ParamConfiguration getRandomConfiguration(MersenneTwisterFast random, boolean allowForbiddenParameters)
	{
		return this.getRandomConfiguration(new RandomAdapter(random), allowForbiddenParameters);
	}
	
	
	/**
	 * Returns a random instance for the configuration space
	 * @param random 	a random object we will use to generate the configurations
	 * @param allowForbiddenParameters  <code>true</code> if we can return parameters that are forbidden, <code>false</code> otherwise.
	 * @return	a random member of the configuration space (each parameter (ignoring the subspace) is sampled uniformly at random, and rejected if it's forbidden
	 */
	private ParamConfiguration getRandomConfiguration(RandomAdapter random, boolean allowForbiddenParameters)
	{
		
		if(random == null) throw new IllegalArgumentException("Cannot supply null random object ");
		
		//fastRand.setSeed(random.nextLong());
		double[] valueArray = new double[numberOfParameters];
		
		for(int j=0; j < 1000000; j++)
		{
			
			for(int i=0; i < numberOfParameters; i++)
			{
				if(searchSubspaceActive[i])
				{		
					valueArray[i] = searchSubspaceValues[i];
				}	else if (parameterDomainContinuous[i])
				{
					NormalizedRange nr = this.normalizedRangesByIndex[i]; 
					valueArray[i] = nr.normalizeValue(nr.unnormalizeValue(random.nextDouble()));
				
				} else
				{
					valueArray[i] = random.nextInt(categoricalSize[i]) + 1;
				}
			}
			
			ParamConfiguration p = new ParamConfiguration(this, valueArray, categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
			if(allowForbiddenParameters || !p.isForbiddenParamConfiguration())
			{
				return p;
			} 
		}
		
		throw new IllegalArgumentException("After 1,000,000 attempts at generating a random configurations we have failed to generate even one that isn't forbidden. It is likely that your forbidden parameter settings are too restrictive. Try excluding smaller regions of the space.");
	}
	
	/**
	 * Returns the default configuration for the Configuration Space
	 * @return	paramconfiguration representing the default
	 */
	public ParamConfiguration getDefaultConfiguration()
	{
		return getConfigurationFromValueArray(this.defaultConfigurationValueArray);
	}
	
	private ParamConfiguration _getDefaultConfiguration()
	{
		
		ParamConfiguration p = new ParamConfiguration(this, new double[numberOfParameters], categoricalSize, parameterDomainContinuous, paramKeyIndexMap); 
		Map<String, String> defaultMap = getDefaultValuesMap();
		p.putAll(defaultMap);	
		return p;
	}
	
	/**
	 * Returns an upper bound on the size of the configuration space. 
	 * There are no guarantees how tight this upper bound might be, and in general it may get looser over time.
	 * You are only guaranteed that the number of actual configurations is LOWER than this upper bound.
	 * <b>NOTE:</b> Search Subspaces do NOT lower the size of the upper bound because you can leave the subspace 
	 * 
	 * @return an upper bound on the size of the search space
	 */
	public double getUpperBoundOnSize()
	{
		//Default cannot be forbidden so there is at least 1 configuration
		//We don't need to worry about the edge case
		double configSpaceSize = 1;
		
		for(int i=0; i < this.numberOfParameters; i++)
		{
			
		
			int catSize = this.categoricalSize[i];

			if(catSize != INVALID_CATEGORICAL_SIZE)
			{
				configSpaceSize *= catSize;
			} else
			{
				
				NormalizedRange nr = this.contNormalizedRanges.get(this.authorativeParameterNameOrder.get(i));
				
				if(nr.isIntegerOnly())
				{
					configSpaceSize *= (nr.unnormalizeValue(1) - nr.unnormalizeValue(0) + 1);
				} else
				{
					return Double.POSITIVE_INFINITY;
				}
				
				
			}
		}
		
		return configSpaceSize;
	}
	
	

	/**
	 * Returns an lower bound on the size of the configuration space. 
	 * There are no guarantees how tight this lower bound might be, and in general it may get looser over time.
	 * You are only guaranteed that the number of actual configurations is HIGHER than this lower bound.
	 * <b>NOTE:</b> Search Subspaces do NOT lower the size of the bound because you can leave the subspace 
	 * 
	 * @return an upper bound on the size of the search space
	 */
	public double getLowerBoundOnSize()
	{
		//Default cannot be forbidden so there is at least 1 configuration
		//We don't need to worry about the edge case
		double configSpaceSize = 1;
		if(this.forbiddenParameterValuesList.size() > 0)
		{
			return 1;
		}
		
		for(int i=0; i < this.numberOfParameters; i++)
		{
		
			int catSize = this.categoricalSize[i];

			if(this.condParents[i].length > 0)
			{
				//Conditionals are ignored
				continue;
			}
					
			if(catSize != INVALID_CATEGORICAL_SIZE)
			{
				
				configSpaceSize *= catSize;
			} else
			{
				
				NormalizedRange nr = this.contNormalizedRanges.get(this.authorativeParameterNameOrder.get(i));
				
				if(nr.isIntegerOnly())
				{
					configSpaceSize *= (nr.unnormalizeValue(1) - nr.unnormalizeValue(0) + 1);
				} else
				{
					return Double.POSITIVE_INFINITY;
				}
			}
		}
		
		return configSpaceSize;
	}
	
	
	
	/**
	 * Generates a configuration with the corresponding valueArray.
	 * 
	 * @param valueArray value array representation of configuration
	 * @return ParamConfiguration object that represents the valueArray
	 */
	private ParamConfiguration getConfigurationFromValueArray(double[] valueArray)
	{
		if(valueArray.length != categoricalSize.length)
		{
			throw new IllegalArgumentException("Value Array Length is not the right size " + valueArray.length + " vs " + categoricalSize.length);
		}
		return new ParamConfiguration(this, valueArray.clone(), categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
	}
	
	public ParamConfiguration getConfigurationFromString( String paramString, StringFormat f)
	{
		return getConfigurationFromString(paramString, f, null);
	}

	public ParamConfiguration getConfigurationFromString( String paramString, StringFormat f, Random rand)
	{
		try 
		{
			
			ParamConfiguration config;
			
			String trySpecialParamString = paramString.trim().toUpperCase();
			//If the string is DEFAULT, <DEFAULT>,RANDOM,<RANDOM> we generate the default or a random as needed
			
			if((trySpecialParamString.equals("DEFAULT") || trySpecialParamString.equals("<DEFAULT>")))
			{
				return this.getDefaultConfiguration();
			}
			
			if((trySpecialParamString.equals("RANDOM") || trySpecialParamString.equals("<RANDOM>")))
			{
				if(rand == null) throw new IllegalStateException("Cannot generate random configurations unless a random object is passed with us");	
				return this.getRandomConfiguration(rand);
			}
			
			double[] valueArray;
			
			ParamConfiguration defaultConfig = this.getDefaultConfiguration();
			
			switch(f)
			{
				case NODB_SYNTAX_WITH_INDEX:
					paramString = paramString.replaceFirst("\\A\\d+:", "");
					//NOW IT'S A REGULAR NODB STRING
				case NODB_SYNTAX:
					valueArray = new double[numberOfParameters];
					config = new ParamConfiguration(this, valueArray,categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
					String tmpParamString = " " + paramString;
					String[] params = tmpParamString.split("\\s-");
					
						
					for(String param : params)
					{
						
						if(param.equals("")) continue;
						String[] paramSplit = param.trim().split(" ");
						if(!paramSplit[1].trim().equals("NaN"))
						{
							config.put(paramSplit[0].trim(),paramSplit[1].replaceAll("'","").trim());
						}
					
						
					}
					
					
					
					break;
				case STATEFILE_SYNTAX_WITH_INDEX:
					paramString = paramString.replaceFirst("\\A\\d+:", "");
				case STATEFILE_SYNTAX:
					valueArray = new double[numberOfParameters];
					config = new ParamConfiguration(this, valueArray,categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
					tmpParamString = " " + paramString.replaceAll("'","");
					params = tmpParamString.split(",");
					for(String param : params)
					{
						if(param.equals("")) continue;
						String[] paramSplit = param.trim().split("=");
						if(!paramSplit[1].trim().equals("NaN"))
						{
							config.put(paramSplit[0].trim(),paramSplit[1].trim());
						}
					}
					
					break;
				case ARRAY_STRING_SYNTAX:
					valueArray = new double[numberOfParameters];
					
					tmpParamString = paramString;
					params = tmpParamString.split(",");
					if(params.length != valueArray.length)
					{
						throw new IllegalArgumentException("Param String Value Array expected to be: " + valueArray.length + " but got a string of length " + paramString.length());
						
						
					}
					
					
					for(int i=0; i < valueArray.length; i++)
					{
						valueArray[i] = Double.valueOf(params[i]);
					}
					config = new ParamConfiguration(this, valueArray,categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
					break;

				case SURROGATE_EXECUTOR:
					valueArray = new double[numberOfParameters];
					config = new ParamConfiguration(this, valueArray,categoricalSize, parameterDomainContinuous, paramKeyIndexMap);
					
					tmpParamString = paramString.trim().replaceAll("-P", "");
					
					params = tmpParamString.split(" "); 
					Set<String> namesSpecified = new HashSet<String>();
					
					for(int i=0; i < params.length; i++)
					{
						String[] param = params[i].split("=");
						
						if(param.length != 2)
						{
							throw new IllegalArgumentException("Param String could not parse portion of string " + paramString + " error occured while seperating: (" + params[i] + ")") ;
						} else
						{
							namesSpecified.add(param[0].trim());
							config.put(param[0].trim(), param[1].trim());
						}
					}
					
				
					
					if(namesSpecified.equals(config.getActiveParameters()))
					{
						break;
					} else
					{
						Set<String> missingButRequired = new HashSet<String>();
						Set<String> specifiedButNotActive = new HashSet<String>();
						missingButRequired.addAll(config.getActiveParameters());
						missingButRequired.removeAll(namesSpecified);
						
						specifiedButNotActive.addAll(namesSpecified);
						specifiedButNotActive.removeAll(config.getActiveParameters());

						throw new IllegalArgumentException("Param String specified some combination of inactive parameters and/or missed active parameters. \nRequired Parameters: " + config.getActiveParameters().size() + "\nSpecified Parameters: " + namesSpecified.size() + "\nRequired But Missing: " + missingButRequired.toString() + "\nSpecified But Not Required" + specifiedButNotActive.toString());
					}
					
				case NODB_OR_STATEFILE_SYNTAX:
					try {
						return getConfigurationFromString(paramString, StringFormat.STATEFILE_SYNTAX, rand);
					} catch(RuntimeException e)
					{
						return getConfigurationFromString(paramString, StringFormat.NODB_SYNTAX, rand);
					}
					
					
				default:
					throw new IllegalArgumentException("Parsing not implemented for String Format" + f);
					
				
				
			}
			
			
			/**
			 * For all inactive parameters, set them to the default value
			 */
			Set<String> allParameters = new HashSet<String>(this.getParameterNames());
			
			allParameters.removeAll(config.getActiveParameters());
			for(String inactiveParameter : allParameters)
			{
				if(config.get(inactiveParameter) == null)
				{
					config.put(inactiveParameter, defaultConfig.get(inactiveParameter));
				}
			}
			
			
			Set<String> missingParameters = new HashSet<String>();
			for(String name : config.keySet())
			{
				if(config.get(name) == null)
				{
					 missingParameters.add(name);
				}
			}
			
			if(missingParameters.size() > 0)
			{
				throw new ParamConfigurationStringFormatException("Error processing Parameter Configuration String \""+ paramString+ "\" in format: "+ f + ". The string specified is missing one or more required parameters: " + missingParameters.toString());
			}
			
			
			
			return config;
		} catch(IllegalStateException e)
		{
			throw e;
		} catch(ParamConfigurationStringFormatException e)
		{
			throw e;
		
		} catch(RuntimeException e )
		{
			throw new ParamConfigurationStringFormatException("Error processing Parameter Configuration String \""+ paramString+ "\" in format: "+ f + " please check the arguments (and nested exception) and try again", e);
		}
		
		
		
	}
	
	public int[] getCategoricalSize()
	{
		return categoricalSize.clone();
	}

	public int[][] getCondParentsArray() {

		return condParents.clone();
	}

	public int[][][] getCondParentValsArray() {

		return condParentVals.clone();
	}
	

	/**
	 * Checks the array representation of a configuration to see if it is forbidden
	 * @param valueArray
	 * @return <code>true</code> if the valueArray is ultimately forbidden, <code>false</code> otherwise.
	 */
	public boolean isForbiddenParamConfiguration(double[] valueArray) {
		
		
		/*
		 * Each value is Nx2 where the first is an index into the array, and the second is the 
		 * index of the categorical value.
		 */
		for(int[][] forbiddenParamValues : forbiddenParameterValuesList)
		{
			
			boolean match = true;
			for(int[] forbiddenParamValue : forbiddenParamValues)
			{
				//Value arrays are indexed by 1, and forbidden parameters are 0 indexed
				if(valueArray[forbiddenParamValue[0]] != forbiddenParamValue[1] + 1)
				{
					match = false;
					break;
				}
				
			}
			if(match)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isIntegerDouble(double d)
	{
		return (d - Math.floor(d) == 0);
	}
	
	/**
	 * @return <code>true</code> if the source of this ParamConfigurationSpace is a real file on disk. 
	 */
	public boolean hasRealParameterFile()
	{
		return hasRealParameterFile;
	}
	
	public static final String SINGLETON_ABSOLUTE_NAME = "<--[SINGLETON SPACE]-->";
	public static final String NULL_ABSOLUTE_NAME ="<--[NULL SPACE]-->";
	/**
	 * Returns a configuration space with a single parameter and single value
	 * 
	 * @return a valid configuration space that is effectively empty
	 */
	public static ParamConfigurationSpace getSingletonConfigurationSpace()
	{
		return new ParamConfigurationSpace(new StringReader("singleton { singleton } [singleton]"),SINGLETON_ABSOLUTE_NAME);
	}
	
	public static ParamConfigurationSpace getNullConfigurationSpace() {
		return new ParamConfigurationSpace(new StringReader(""),NULL_ABSOLUTE_NAME);
	}
	

	void setValueInArray(double[] valueArray, String key, String newValue) {
		
		/* We find the index into the valueArray from paramKeyIndexMap,
		 * then we find the new value to set from it's position in the getValuesMap() for the key. 
		 * NOTE: i = 1 since the valueArray numbers elements from 1
		 */
		
		
		Integer index = paramKeyIndexMap.get(key);
		if(index == null)
		{
			throw new IllegalArgumentException("This key does not exist in the Parameter Space: " + key);

		}
		
		if(newValue == null)
		{
			valueArray[index] = Double.NaN;
		}
		else if(parameterDomainContinuous[index])
		{
			NormalizedRange nr = getNormalizedRangeMap().get(key);
			valueArray[index] = nr.normalizeValue(nr.unnormalizeValue(nr.normalizeValue(Double.valueOf(newValue))));
			
		} else
		{
			List<String> inOrderValues = getValuesMap().get(key);
			int i=1;		
			boolean valueFound = false;
			
			
			for(String possibleValue : inOrderValues)
			{
				if (possibleValue.equals(newValue))
				{
					valueArray[index] = i;
					valueFound = true;
					break;
				} 
				i++;
			}
			
			if(valueFound == false)
			{
				throw new IllegalArgumentException("Value is not legal for this parameter: " + key + " Value:" + newValue);
			}
			
			
		}
		
	}
	
	
	private static class RandomAdapter
	{
		
		private MersenneTwisterFast fastRand;
		private Random rand;

		RandomAdapter(Random r)
		{
			this.rand = r;
			this.fastRand = null;
		}
		
		RandomAdapter(MersenneTwisterFast fastRand)
		{
			this.rand = null;
			this.fastRand =  fastRand;
		}
		
		public int nextInt(int n)
		{
			if(fastRand != null)
			{
				return fastRand.nextInt(n); 
			} else
			{
				return rand.nextInt(n);
			}
			
		}
		
		public double nextDouble()
		{
			if(fastRand != null)
			{
				return fastRand.nextDouble();
			} else
			{
				return rand.nextDouble();
			}
		}
	
	}



	
}


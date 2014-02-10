package ca.ubc.cs.beta.aclib.configspace;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;


/**
 * This class represents an element in the associated {@link ParamConfigurationSpace} and provides a natural {@link Map} like interface for accessing it's members 
 * but also uses an effective and fast storage mechanism for this. 
 * <p>
 * <b>WARNING:</b>This is neither a general purpose <code>Map</code> implementation, nor a complete one. 
 * <p>
 * Differences between this and <code>Map</code>:
 * <p>
 * 1) The key and value space are fixed for each parameter to the corresponding ParamConfigurationSpace <br/>
 * 2) You cannot remove keys, nor can you add keys that don't exist. (i.e. you can only replace existing values)</br>
 * 3) The fastest way to iterate over this map is through <code>keySet()</code>.</br>
 * 4) EntrySet and valueSet() are not implemented, size() is constant and unaffected by removing a key<br/>
 * 5) Two objects are considered equal if and only if all there active parameters are equal. 
 * 
 * <p><b>Thread Safety:</b>This class is NOT thread safe under mutations but typically
 * the lifecycle of a ParamConfiguration object is that it is created with specific values,
 * and then never modified again. This class is thread safe under non-mutation operations.
 * 
 *
 */
@NotThreadSafe
public class ParamConfiguration implements Map<String, String>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 879997991870028528L;

	/**
	 *  Stores a map of paramKey to index into the backing arrays
	 */
	private final Map<String, Integer> paramKeyToValueArrayIndexMap;
	
	/**
	 * @see ParamConfigurationSpace
	 * Do _NOT_ write to this array, it is shared with all other configurations 
	 */
	private final boolean[] parameterDomainContinuous;
	
	/**
	 * For each numerical index in the backing array, if categorical what is the size of the domain.
	 * Do _NOT_ write to this array, it is shared with all other configurations
	 */
	private final int[] categoricalSize;
	

	/**
	 * Configuration space we are from
	 */
	private final ParamConfigurationSpace configSpace;
	
	/**
	 * The array that actually stores our values. We store categorical values as their index in
	 * the configSpace.getValues( key )
	 * <p>
	 * e.g. if the List contains three values "foo", "bar" and "dog", and we want to store "bar"
	 * we would store 2.0. 
	 * <p>
	 * For continuous parameters we just store there raw value.
	 * <p>
	 * For non active parameters we store a NaN
	 */
	private final double[] valueArray;
	
	/** 
	 * Stores whether the map has been changed since the previous read
	 * 
	 * Prior to a read if this is true, we will call cleanUp()
	 */
	private volatile boolean isDirty = true; 
	
	
	/**
	 * Stores whether the parameter in the array is active or not (i.e. are all the parameters it is conditional upon set correctly).
	 * This value is lazily updated whenever it is read if this configuration is marked dirty.
	 */
	private final boolean[] activeParams;

	
	/**
	 * Value array used in comparisons with equal() and hashCode()
	 * (All Inactive Parameters are hidden)
	 */
	private final double[] valueArrayForComparsion; 

	/**
	 * Default Constructor 
	 * @param configSpace 					paramconfigurationspace we are from
	 * @param valueArray 					array that represents our values. (DO NOT MODIFY THIS)
	 * @param categoricalSize 				array that has the size of the domain for categorical variables. (DO NOT MODIFY THIS) 
	 * @param parameterDomainContinuous		array that tells us whether an entry in the value array is continuous. (DO NOT MODIFY THIS)
	 * @param paramKeyToValueArrayIndexMap	map from param keys to index into the value arry.
	 */
	ParamConfiguration(ParamConfigurationSpace configSpace ,double[] valueArray, int[] categoricalSize, boolean[] parameterDomainContinuous, Map<String, Integer> paramKeyToValueArrayIndexMap )
	{
		this.configSpace = configSpace;
		this.valueArray = valueArray;
		
		this.categoricalSize = categoricalSize;
		this.parameterDomainContinuous = parameterDomainContinuous;
		this.paramKeyToValueArrayIndexMap = paramKeyToValueArrayIndexMap;
		this.myID = idPool.incrementAndGet();
		this.activeParams = new boolean[valueArray.length];
		isDirty = true;
		this.valueArrayForComparsion = new double[valueArray.length];
	}
		
	
	
	/**
	 * Copy constructor
	 * @param oConfig - configuration to copy
	 */
	public ParamConfiguration(ParamConfiguration oConfig)
	{
		if(oConfig.isDirty) oConfig.cleanUp();
		
		this.isDirty = oConfig.isDirty;
		
		this.myID = oConfig.myID;
		this.configSpace = oConfig.configSpace;
		this.valueArray = oConfig.valueArray.clone();
		this.categoricalSize = oConfig.categoricalSize;
		this.parameterDomainContinuous = oConfig.parameterDomainContinuous;
		this.paramKeyToValueArrayIndexMap = oConfig.paramKeyToValueArrayIndexMap;
		
		this.activeParams = oConfig.activeParams.clone();
		
		this.valueArrayForComparsion = oConfig.valueArrayForComparsion.clone();
		this.lastHash = oConfig.lastHash;
		
	}
	
	@Override
	public int size() {
		return valueArray.length;
	}

	@Override
	public boolean isEmpty() {
		return (valueArray.length == 0);
	}

	@Override
	public boolean containsKey(Object key) {			
		return ((paramKeyToValueArrayIndexMap.get(key) != null) && (valueArray[paramKeyToValueArrayIndexMap.get(key)] != 0));
	}


	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	

	@Override
	public String get(Object key) {
		
		Integer index = paramKeyToValueArrayIndexMap.get(key);
		if(index == null)
		{
			return null;
		}
		
		
		
		double value = valueArray[index];
		
		if(Double.isNaN(value))
		{
			return null;
		}
		
		if(parameterDomainContinuous[index])
		{
			NormalizedRange range = configSpace.getNormalizedRangeMap().get(key);
			if(range.isIntegerOnly())
			{
				return String.valueOf((long) Math.round(range.unnormalizeValue(value)));
			} else
			{
				return String.valueOf(range.unnormalizeValue(value));
			}
			
		} else
		{
			if(value == 0)
			{
				return null;
			} else
			{		
				return configSpace.getValuesMap().get(key).get((int) value - 1);
			}
		}
	}



	/**
	 * Replaces a value in the Map
	 * 
	 * <b>NOTE:</b> This operation can be slow and could be sped up if the parser file had a Map<String, Integer> mapping Strings to there integer equivilants.
	 * Also note that calling this method will generally as a side effect change the FriendlyID of this ParamConfiguration Object
	 * 
	 * 
	 * @param key string name to store
	 * @param newValue string value to store
	 * @return previous value in the array
	 */
	public String put(String key, String newValue) 
	{
	
		/* We find the index into the valueArray from paramKeyIndexMap,
		 * then we find the new value to set from it's position in the getValuesMap() for the key. 
		 * NOTE: i = 1 since the valueArray numbers elements from 1
		 */
		
		isDirty = true;

		Integer index = paramKeyToValueArrayIndexMap.get(key);
		if(index == null)
		{
			throw new IllegalArgumentException("This key does not exist in the Parameter Space: " + key);

		}
		
		String oldValue = get(key);
		
		if(newValue == null)
		{
			valueArray[index] = Double.NaN;
		}
		else if(parameterDomainContinuous[index])
		{
			valueArray[index] = configSpace.getNormalizedRangeMap().get(key).normalizeValue(Double.valueOf(newValue));
			
		} else
		{
			List<String> inOrderValues = configSpace.getValuesMap().get(key);
			int i=1;		
			boolean valueFound = false;
			
			
			for(String possibleValue : inOrderValues)
			{
				if (possibleValue.equals(newValue))
				{
					this.valueArray[index] = i;
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
		
	
		
		if(parameterDomainContinuous[index] && newValue != null)
		{
			double d1 = Double.valueOf(get(key));
			double d2 = Double.valueOf(newValue);
			
			if(Math.abs(d1/d2 - 1) >  Math.pow(10, -12))
			{
				System.out.println("Warning got the following value back from map " + get(key) + " put " + newValue + " in");
			}
				//throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);
		} else
		{
			if(get(key) == null)
			{
				if(newValue != null)
				{
					throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);	
				}
			} else if(!get(key).equals(newValue))
			{
				throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);
			}
		}
		return oldValue;
	}

	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public String remove(Object key) {
	
		throw new UnsupportedOperationException();
	}


	@Override
	public void putAll(Map<? extends String, ? extends String> m) {

		
		for(Entry<? extends String, ? extends  String> ent : m.entrySet())
		{
			this.put(ent.getKey(), ent.getValue());
		}
	
		
		
	}


	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public void clear() {
		throw new UnsupportedOperationException();
		
	}


	@Override
	/**
	 * Returns a Set that will iterate in the order
	 */
	public Set<String> keySet() {
		LinkedHashSet<String> keys = new LinkedHashSet<String>();
		for(String s : paramKeyToValueArrayIndexMap.keySet())
		{
			keys.add(s);
		}
		
		return keys;
	}


	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public Collection<String> values() {
		throw new UnsupportedOperationException();
	}


	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns a copy of the value array
	 * @return clone of the value array with inactive values intact
	 */
	public double[] toValueArray()
	{
		return valueArray.clone();
	}
	
	
	public double[] toComparisonValueArray() {
		if(isDirty) cleanUp();
		
		return valueArrayForComparsion.clone();
	}
	
	
	@Override
	public String toString()
	{
		if(isDirty) cleanUp();
		
		
	
		return getFriendlyIDHex();
	}
	
	/**
	 * Two instances are equal if they come from the same configuration space and their active parameters have the same values
	 * 
	 * <b>Note:</b> Integer value parameters will fail this test.
	 * 
	 * @param o object to check equality with
	 */
	
	public boolean equals(Object o)
	{

		if(this == o) return true;
		if (o instanceof ParamConfiguration)
		{
			
			ParamConfiguration opc = (ParamConfiguration )o;
			if(isDirty) cleanUp();
			if(opc.isDirty) opc.cleanUp();
			
			if(!configSpace.equals(opc.configSpace)) return false;
			
			
			for(int i=0; i < valueArrayForComparsion.length; i++)
			{

				if(Math.abs(valueArrayForComparsion[i] - opc.valueArrayForComparsion[i]) > EPSILON)
				{
					return false;
				}
			}
			
			return true;
		} else
		{
			return false;
		}
	}
	
	boolean hashSet = false;
	int lastHash = 0;
	
	@Override
	public int hashCode()	
	{ 
		if(isDirty || !hashSet)
		{
			if(isDirty) cleanUp();
			
			
			float[] values = new float[valueArrayForComparsion.length];
			
			
			for(int i=0; i < values.length; i++)
			{
				values[i] = (float) valueArrayForComparsion[i];
				
			}
			
			lastHash = Arrays.hashCode(values);
			//lastHash = Hash.hashCode(values); 
			
			
			hashSet = true;
		}
		
		
		return lastHash;
		
		
		
	}
	
	
	/**
	 * Builds a formatted string consisting of the active parameters 
	 * 
	 * @param preKey - String to appear before the key name
	 * @param keyValSeperator - String to appear between the key and value
	 * @param valueDelimiter - String to appear on either side of the value
	 * @param glue - String to placed in between various key value pairs
	 * @return formatted parameter string 
	 * @deprecated Clients should always specify a String Format {@link #getFormattedParamString(StringFormat)}
	 */
	@Deprecated
	public String getFormattedParamString(String preKey, String keyValSeperator,String valueDelimiter,String glue)
	{
		//Should use the String Format method
		return _getFormattedParamString(preKey, keyValSeperator, valueDelimiter, glue, true);
	
		
	}
	/**
	 * Converts configuration into string format with the given tokens
	 * 
	 * @param preKey 					string that occurs before a key
	 * @param keyValSeperator 			string that occurs between the key and value
	 * @param valueDelimiter 			string that occurs on either side of the value
	 * @param glue 						string that occurs between pairs of key values
	 * @param hideInactiveParameters	<code>true</code> if we should drop inactive parameters, <code>false</code> otherwise
	 * @return formatted parameter string
	 */
	protected String _getFormattedParamString(String preKey, String keyValSeperator,String valueDelimiter,String glue, boolean hideInactiveParameters)
	{
		Set<String> activeParams = getActiveParameters();
		StringBuilder sb = new StringBuilder();
		boolean isFirstParameterInString = true;
		
		for(String key : keySet())
		{
			if(get(key) == null) continue;
			if((!activeParams.contains(key)) && hideInactiveParameters) continue;
			if(!isFirstParameterInString)
			{
				sb.append(glue);
			}
			isFirstParameterInString = false;
			sb.append(preKey).append(key).append(keyValSeperator).append(valueDelimiter).append(get(key)).append(valueDelimiter);
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string representation of this object
	 * @deprecated Clients should always specify a String Format {@link #getFormattedParamString(StringFormat)}
	 * @return string representation of this object
	 */
	@Deprecated
	public String getFormattedParamString()
	{
		return _getFormattedParamString("-", " ","'"," ",true);
	}
	
	/**
	 * Returns a string representation of this object, according to the given {@link StringFormat}
	 * <br/>
	 * <b>Implementation Note:</b>No new String Formats should be able to generate the Strings "DEFAULT","<DEFAULT>","RANDOM","<RANDOM>", no matter how obnoxious the user specifying the param file is
	 * @param stringFormat stringformat to use
	 * @return string representation
	 */
	public String getFormattedParamString(StringFormat stringFormat)
	{
		
		double[] valueArray = this.valueArray;
		switch(stringFormat)
		{
			case FIXED_WIDTH_ARRAY_STRING_MASK_INACTIVE_SYNTAX:
				if(isDirty) cleanUp();
				valueArray = this.valueArrayForComparsion;
				
			case FIXED_WIDTH_ARRAY_STRING_SYNTAX:
				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);
				
				for(int i=0; i < valueArray.length; i++)
				{
					pWriter.format("%20s", valueArray[i]);
					if(i+1 != valueArray.length) pWriter.append(",");
				}
				return sWriter.toString();
			
		
		
			case ARRAY_STRING_MASK_INACTIVE_SYNTAX:
				if(isDirty) cleanUp();
				valueArray = this.valueArrayForComparsion;
				
			case ARRAY_STRING_SYNTAX:
				StringBuilder sb = new StringBuilder();
				for(int i=0; i < valueArray.length; i++)
				{
					sb.append(valueArray[i]);
					if(i+1 != valueArray.length) sb.append(",");
				}
				return sb.toString();
				
			case NODB_OR_STATEFILE_SYNTAX:
				return getFormattedParamString(StringFormat.NODB_SYNTAX);
			//case SILLY:
			//	return "RANDOM";
		default:
			return _getFormattedParamString(stringFormat.getPreKey(), stringFormat.getKeyValueSeperator(), stringFormat.getValueDelimeter(), stringFormat.getGlue(), stringFormat.hideInactiveParameters());
		}
		
	}
	
	
	/**
	 * Stores information about the various string formats we support
	 * <p>
	 * <b>Note:</b> Only some of these use the preKey, keyVal seperator, and glue 
	 * <b>WARNING:</b>DEFAULT, &gt;DEFAULT&lt;,RANDOM, &gt;RANDOM&lt; cannot be valid configuration strings for all format, because we will always parse these back as the default or random configuration respectively.
	 */
	public enum StringFormat
	{
			/**
			 * Uses a -(name) 'value' -(name) 'value' ... format [hiding inactive parameters]
			 */
			NODB_SYNTAX("-"," ", "'", " ", true), 
			/**
			 * Stores a number and colon before entry of <code>NODB_SYNTAX</code>. Used only for deserializing
			 */
			NODB_SYNTAX_WITH_INDEX("-"," ", "'", " ", true),
			/**
			 * Stores in a name='value',name='value'... format [preserving inactive parameters]
			 */
			STATEFILE_SYNTAX(" ","=","'",",",false), 
			/**
			 * Stores a number and colon before an entry of <code>STATEFILE_SYNTAX</code>. Used only for deserializing
			 */
			STATEFILE_SYNTAX_WITH_INDEX(" ", "=","'",",", false),
			/**
			 * Stores in a name='value',name='value'... format [removing inactive parameters]
			 */
			STATEFILE_SYNTAX_NO_INACTIVE(" ", "=" , "'" , ",",true),
			
			/**
			 * Uses a -Pname=value -Pname=value -Pname=value format
			 */
			SURROGATE_EXECUTOR("-P","=",""," ",true),
			
		
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile
			 */
			ARRAY_STRING_SYNTAX("","","","",false),
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile (hides Inactive)
			 */
			ARRAY_STRING_MASK_INACTIVE_SYNTAX("","","","", true),
			
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile
			 * All values are spaced to take 15 characters
			 */
			FIXED_WIDTH_ARRAY_STRING_SYNTAX("","","","",false),
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile (hides Inactive)
			 * All values are spaced to take 15 characters
			 */
			FIXED_WIDTH_ARRAY_STRING_MASK_INACTIVE_SYNTAX("","","","", true),
			
			/**
			 * Stores values in a NODB syntax, parses values from either.
			 */
			NODB_OR_STATEFILE_SYNTAX("","","","",true)
			
			//SILLY("","","","",false)
			;
			
			
			
			
			
		private final String preKey;
		private final String keyValSeperator;
		private final String valDelimiter;
		private final String glue;
		private final boolean hideInactive;
		
		
		private StringFormat(String preKey, String keyValSeperator, String valDelimeter, String glue, boolean hideInactive)
		{
			this.preKey = preKey;
			this.keyValSeperator = keyValSeperator;
			this.valDelimiter = valDelimeter;
			this.glue = glue;
			this.hideInactive = hideInactive;
		}

		public boolean hideInactiveParameters() {
			return hideInactive;
		}

		public String getPreKey() {
			return preKey;
		}
		
		public String getGlue()
		{
			return glue;
		}
		
		public String getValueDelimeter()
		{
			return valDelimiter;
		}
		
		public String getKeyValueSeperator()
		{
			return keyValSeperator;
		}
	
	}
	
	
	
	/**
	 * Returns a list of configurations in the neighbourhood of this one (Forbidden Configurations are excluded)
	 * @param  rand 					An object that will be used to generate neighbours for numerical parameters.
	 * @param  numNumericalNeighbours 	The number of neighbours numerical parameters should have
	 * @return list of configurations in the neighbourhood
	 */
	public List<ParamConfiguration> getNeighbourhood(Random rand, int numNumericalNeighbours)
	{
		List<ParamConfiguration> neighbours = new ArrayList<ParamConfiguration>(numberOfNeighboursExcludingForbidden(numNumericalNeighbours));
		Set<String> activeParams = getActiveParameters();
		/*
		 * i is the number of parameters
		 * j is the number of neighbours
		 */
		for(int i=0; i < configSpace.getParameterNamesInAuthorativeOrder().size(); i++)
		{
			double[] newValueArray = valueArray.clone();
			
			for(int j=1; j <= numberOfNeighboursForParam(i,activeParams.contains(configSpace.getParameterNamesInAuthorativeOrder().get(i)),numNumericalNeighbours); j++)
			{
				newValueArray[i] = getNeighbourForParam(i,j,rand);
				
				if(configSpace.isForbiddenParamConfiguration(newValueArray)) continue;
				
				neighbours.add(new ParamConfiguration(configSpace, newValueArray.clone(), categoricalSize, parameterDomainContinuous, paramKeyToValueArrayIndexMap));
			}
		}
		
		
		if(neighbours.size() > numberOfNeighboursExcludingForbidden(numNumericalNeighbours))
		{
			throw new IllegalStateException("Expected " + numberOfNeighboursExcludingForbidden(numNumericalNeighbours) + " neighbours (should be greater than or equal to) but got " + neighbours.size());
		}
		return neighbours;
		
		
	}
	
	/**
	 * Returns <code>true</code> if the parameters differ in exactly 1 place (only considers active parameters)
	 * 
	 * @param oConfig 	the other configuration to check
	 * @return
	 */
	public boolean isNeighbour(ParamConfiguration oConfig)
	{
		
		if(!oConfig.getConfigurationSpace().equals(getConfigurationSpace()))
		{
			return false;
		}
		
		if(isDirty) cleanUp();		
		if(oConfig.isDirty) oConfig.cleanUp();
		
		
		int differences = 0;
		for(int i=0; i < this.activeParams.length; i++)
		{
			if(this.activeParams[i] && oConfig.activeParams[i])
			{
				
				if(this.valueArrayForComparsion[i] != oConfig.valueArrayForComparsion[i])
				{
					differences++;
				}
			}
		}
		
		return (differences == 1) ? true : false;
	}
	
	
	/**
	 * Returns the number of neighbours for this configuration
	 * @return number of neighbours that this configuration has
	 */
	private int numberOfNeighboursExcludingForbidden(int numNumericalNeighbours)
	{
		int neighbours = 0;
		
		Set<String> activeParams = getActiveParameters();
		
		for(int i=0; i < configSpace.getParameterNamesInAuthorativeOrder().size(); i++)
		{
			
			neighbours += numberOfNeighboursForParam(i, activeParams.contains(configSpace.getParameterNamesInAuthorativeOrder().get(i)), numNumericalNeighbours);
		}
		return neighbours;
		
	}
	
	/**
	 * Returns the number of Neighbours for the specific index into the param Array
	 * @param valueArrayIndex	index into the valueArray for this parameter
	 * @param isParameterActive boolean for if this parameter is active
	 * @return 0 if inactive, number of neighbours if active
	 */
	private int numberOfNeighboursForParam(int valueArrayIndex, boolean isParameterActive, int neighboursForNumericalParameters)
	{
		if(isParameterActive == false) return 0;
		
		if(configSpace.searchSubspaceActive[valueArrayIndex]) return 0;
		
		if(parameterDomainContinuous[valueArrayIndex])
		{
		  return neighboursForNumericalParameters;
		} else
		{
		  return categoricalSize[valueArrayIndex] - 1;
		}
	}
	
	/**
	 * Returns a neighbour for this configuration in array format
	 * 
	 * @param valueArrayIndex   index of the parameter which we are generating a neighbour for
	 * @param neighbourNumber   number of the neighbour to generate
	 * @return
	 */
	private double getNeighbourForParam(int valueArrayIndex, int neighbourNumber, Random rand)
	{
		if(parameterDomainContinuous[valueArrayIndex])
		{ 
			//Continuous arrays sample from a
			//normal distrbution with mean valueArray[i] and stdDeviation 0.2
			//0.2 is simply a magic constant
			double mean = valueArray[valueArrayIndex];
			
			
			
			while(true)
			{
				double randValue = 0.2*rand.nextGaussian() + mean;
				
				if(randValue >= 0 && randValue <= 1)
				{
					return randValue;
				}
			}
		}  else
		{ 
			//For categorical parameters we return the number of the neighbour 
			//up to our value in the parameter, and then 1 more than this after
			
			//e.g. if our value was 2 we would return
			//  0 => 0
			//  1 => 1 
	        //  2 => 3
			//  3 => 4
			//  ...
			if(neighbourNumber < valueArray[valueArrayIndex])
			{
				return neighbourNumber;
			} else
			{
				return neighbourNumber+1;
			}
		}
	}
	
	/**
	 * Recomputes the active parameters and valueArrayForComparision and marks configuration clean again
	 * We also change our friendly ID
	 */
	private void cleanUp()
	{	
		
		Set<String> activeParams = getActiveParameters();
		
		for(Entry<String, Integer> keyVal : this.paramKeyToValueArrayIndexMap.entrySet())
		{
			
			
			this.activeParams[keyVal.getValue()] = activeParams.contains(keyVal.getKey()); 
			
			
			if(this.activeParams[keyVal.getValue()])
			{
				if(this.parameterDomainContinuous[keyVal.getValue()])
				{
					this.valueArrayForComparsion[keyVal.getValue()] = valueArray[keyVal.getValue()];
				} else
				{
					this.valueArrayForComparsion[keyVal.getValue()] = valueArray[keyVal.getValue()];
				}
				

			} else
			{
				this.valueArrayForComparsion[keyVal.getValue()] = Double.NaN;
			}
		}
		myID = idPool.incrementAndGet();
		isDirty = false;
	}
	
	/**
	 * Returns the keys that are currently active
	 * @return set containing the key names that are currently active
	 */
	public Set<String> getActiveParameters()
	{
		boolean activeSetChanged = false;
		Set<String> activeParams= new HashSet<String>();
		
		/*
		 * This code is will loop in worse case ~(n^2) times, the data structures may not be very 
		 * good either, so gut feeling is probably Omega(n^3) in worse case.
		 * 
		 *  This algorithm basically does the following:
		 *  1) Adds all independent clauses to the active set
		 *  2) For every dependent value:
		 *  	- checks if each dependee parameter is active
		 *  	- if all dependee parameters have an acceptible value, adds it to the active set.
		 *  3) Terminates when there are no changes to the active set. (controlled by the changed flag) 
		 */
		do {
			/*
			 * Loop through every parameter to see if it should be added to the activeParams set.
			 */
			activeSetChanged = false;
			List<String> paramNames = this.configSpace.getParameterNames();
			
			for(String candidateParam : paramNames)
			{	
				
				if(activeParams.contains(candidateParam))
				{ 
					//We already know the Param is active
					continue;
				}
				
				
				/*
				 * Check if this parameter is conditional (if not add it to the activeParam set), if it is check if it's conditions are all satisified. 
				 */	
				Map<String,List<String>> dependentOn;
				if(( dependentOn = configSpace.getDependentValuesMap().get(candidateParam)) != null)
				{
					//System.out.print(" is dependent ");
					
					
					boolean dependentValuesSatified = true; 
					for(String dependentParamName : dependentOn.keySet())
					{
						if(activeParams.contains(dependentParamName))
						{
							if(dependentOn.get(dependentParamName).contains(get(dependentParamName)))
							{	
								//System.out.print("[+]:" +  dependentParamName +  " is " + params.get(dependentParamName)); 
							} else
							{	
								//System.out.print("[-]:" + dependentParamName +  " is " + params.get(dependentParamName));
								dependentValuesSatified = false;
								break;
							}
								
						} else
						{
							dependentValuesSatified = false;
							break;		
						}
					}
					
					if(dependentValuesSatified == true)
					{
						
						activeSetChanged = true;
						activeParams.add(candidateParam);
					} else
					{
						
					}
					
					
					
				} else
				{ //This Parameter is not dependent
					if(activeParams.add(candidateParam))
					{
						activeSetChanged = true;
						
					}
				}				
			}

			
		} while(activeSetChanged == true);
		
		
		return activeParams;
	}
	
	
	
	private static final AtomicInteger idPool = new AtomicInteger(0);
	private int myID;
	/**
	 * Friendly IDs are just unique numbers that identify this configuration for logging purposes
	 * you should never rely on this for programmatic purposes. 
	 * 
	 * If you change the configuration the id will be regenerated.
	 * 
	 * <b>Note</b>: While a given ID should refer to a specific configuration, the converse is not true.
	 * 
	 * @return unique id for this param configuration
	 */
	public int getFriendlyID() {
		
		if(isDirty) cleanUp();
		
		return myID;
	}

	public String getFriendlyIDHex()
	{
		String hex = Integer.toHexString(getFriendlyID());
		
		StringBuilder sb = new StringBuilder("0x");
		while(hex.length() + sb.length() < 6)
		{
			sb.append("0");
		}
		sb.append(hex.toUpperCase());
		return sb.toString();
	}
	/**
	 * Checks whether this configuration is forbidden
	 * @return <code>true</code> if the parameter is forbidden, false otherwise
	 */
	public boolean isForbiddenParamConfiguration()
	{
		return configSpace.isForbiddenParamConfiguration(valueArray);
	}


	/**
	 * Returns the configuration space for this configuartion
	 * @return configSpace for this configuration
	 */ 
	public ParamConfigurationSpace getConfigurationSpace() {
		return configSpace;
	}

	

	private static final double EPSILON = Math.pow(10, -14);

	public boolean isInSearchSubspace() {
		
		for(int i=0; i < valueArray.length; i++)
		{
			if(configSpace.searchSubspaceActive[i])
			{
				if(Math.abs(valueArray[i]-configSpace.searchSubspaceValues[i]) > EPSILON)
				{
					return false;
				}
			}
		}
		
		return true;
	}



	



	

}

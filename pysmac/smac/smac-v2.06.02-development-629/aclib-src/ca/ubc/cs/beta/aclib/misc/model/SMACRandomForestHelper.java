package ca.ubc.cs.beta.aclib.misc.model;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.options.RandomForestOptions;
import ca.ubc.cs.beta.models.fastrf.RegtreeBuildParams;
/**
 * Utility class that converts the RandomForestOptions object into a RegtreeBuildParams objects
 * @author sjr
 *
 */
public class SMACRandomForestHelper {

	private static final Logger log = LoggerFactory.getLogger(SMACRandomForestHelper.class);
	
	/**
	 * Converts the rfOptions and other parameters into the required RegtreeBuildParams
	 * 
	 * @param rfOptions options object specifying settings for RandomForest construction
	 * @param numberOfFeatures   number of features we will build with
	 * @param categoricalSize	 sizes of the categorical values
	 * @param condParents		 for each parameter specifies the indexes of other parameters we are dependent upon
	 * @param condParentVals	 for each parameter specifies the value required for the indepnedent parameters for this parameter to be activee
	 * @return regtreeBuildParams object for Random Forest construction
	 */
	public static RegtreeBuildParams getRandomForestBuildParams(RandomForestOptions rfOptions, int numberOfFeatures, int[] categoricalSize, int[][] condParents, int[][][] condParentVals, Random rand)
	{
	/*
	 * Parameter File Generator
	 */
	RegtreeBuildParams buildParams = new RegtreeBuildParams();
	
	buildParams.condParents = null;
	/*
	 * Most of the defaults are either read from the config or were 
	 * pilfered from a run of the MATLAB
	 * The actual values may need to be more intelligently chosen.
	 */
	buildParams.splitMin = rfOptions.splitMin;
	buildParams.ratioFeatures = rfOptions.ratioFeatures;//(5.0/6);
	
	buildParams.logModel = ((rfOptions.logModel == null) ? 1 :(((rfOptions.logModel) ? 1 : 0)));
	buildParams.storeResponses = rfOptions.storeDataInLeaves;
	buildParams.random = rand;
	//System.out.println("Random: " + buildParams.random.nextInt());
	buildParams.minVariance = rfOptions.minVariance;
	
	if(rfOptions.brokenVarianceCalculation)
	{
		log.warn("Model set to use broken variance calculation, this may affect performance");
		buildParams.brokenVarianceCalculation = true;
	} else
	{
		buildParams.brokenVarianceCalculation = false;
	}
	
	//int numberOfParameters = params.getParameterNames().size();
	//int numberOfFeatures = features.getDataRow(0).length;
	
	/**
	 * THis needs to be the length of the number of parameters in a configuration + the number of features in a configuration
	 */
	
	
	buildParams.catDomainSizes = new int[categoricalSize.length+ numberOfFeatures];
	System.arraycopy(categoricalSize, 0, buildParams.catDomainSizes, 0, categoricalSize.length);
	
	
	//buildParams.catDomainSizes[i] = 0;
	
	
	
	buildParams.condParents = new int[categoricalSize.length+numberOfFeatures][];
	for(int i=0; i < categoricalSize.length; i++)
	{
		buildParams.condParents[i] = condParents[i];
	}
	
	
	buildParams.condParentVals = new int[categoricalSize.length+numberOfFeatures][][];
	
	for(int i=0; i < condParentVals.length; i++)
	{
		buildParams.condParentVals[i] = condParentVals[i];
	}
	
	for(int i=categoricalSize.length; i < buildParams.condParents.length; i++)
	{
		buildParams.condParents[i] = new int[0];
		buildParams.condParentVals[i] = new int[0][0];
	}

	if(rfOptions.ignoreConditionality)
	{
		//TODO: Make this a ModelDataSanitizer
		buildParams.condParents = null;
		buildParams.condParentVals = null;
	}

	return buildParams;	
	}
}

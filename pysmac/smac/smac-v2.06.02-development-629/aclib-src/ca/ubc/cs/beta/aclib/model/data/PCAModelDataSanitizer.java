package ca.ubc.cs.beta.aclib.model.data;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.misc.math.ArrayMathOps;
import ca.ubc.cs.beta.aclib.misc.math.MessyMathHelperClass;
import ca.ubc.cs.beta.aclib.misc.math.MessyMathHelperClass.Operation;

/**
 * This class roughly does all the processing for sanitizing data
 * @author seramage
 *
 */
public class PCAModelDataSanitizer extends AbstractSanitizedModelData {

	
	private final double[][] pcaVec;
	private final double[] pcaCoeff;
	private final int[] sub;
	private final double[] means;
	private final double[] stdDev;
	private final double[][] pcaFeatures;
	private final double[][] prePCAInstanceFeatures;
	private final double[] responseValues;
	private final ParamConfigurationSpace configSpace;
	private final double[][] configs;
	
	private final boolean logModel;
	/**
	 * Debugging crap that basically writes the arguments to a file that you can then use to test outside of Matlab
	 */
	public static int index = 0;
	public static final String filename = "/tmp/lastoutput-mds";
	static boolean writeOutput = true;
	private Logger log = LoggerFactory.getLogger(getClass());
	private int[][] theta_inst_idxs;
	private boolean[] censoredResponseValues;
//	
//	public static void main(String[] args)
//	{
//		for(int i=0; i < 10; i++)
//		{
//			/*
//			double[][] m1 = {{ 1,2},{3,4},{5,6}};
//			double[][] m2 = {{1,2,3},{4,5,6}};
//			System.out.println(explode(Arrays.deepToString((new MessyMathHelperClass()).matrixMultiply(m1, m2))));
//			 */
//			File f = new File(filename + "-" + 1);
//			ObjectInputStream in;
//			try {
//				in = new ObjectInputStream(new FileInputStream(f));
//			
//			double[][] instanceFeatures  = (double[][]) in.readObject();
//			double[][] paramValues = (double[][]) in.readObject();
//			double[] responseValues = (double[]) in.readObject();
//			int[] usedInstances = (int[]) in.readObject();
//			in.close();
//		
//			writeOutput = false;
//			
//			int numPCA = 7;
//			
//			boolean logModel = true;
//			
//			
//			SanitizedModelData mdc = new PCAModelDataSanitizer(instanceFeatures, paramValues, numPCA, responseValues, usedInstances, logModel);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
//	}
	
	public static String explode(String s)
	{
		return s.replaceAll("]","}\n").replaceAll("\\[", "{");
	}
	
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues, int[] usedInstances, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues )
	{
		this(instanceFeatures, paramValues, numPCA, responseValues, usedInstances, logModel,  theta_inst_idxs,  censoredResponseValues , null);
	}
	
	public static boolean printFeatures = false;
	
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues, int[] usedInstancesIdxs, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues , ParamConfigurationSpace configSpace)
	{
		this.configSpace = configSpace;
		this.configs = paramValues;
		this.responseValues = responseValues;
		this.theta_inst_idxs = theta_inst_idxs;
		this.censoredResponseValues = censoredResponseValues;
		
		this.prePCAInstanceFeatures = ArrayMathOps.copy(instanceFeatures);
		
		/*
		if(RoundingMode.ROUND_NUMBERS_FOR_MATLAB_SYNC)
		{
			if(!printFeatures)
			{
				for(int i=0; i < instanceFeatures.length; i++)
				{
					System.out.println(i+":" + Arrays.toString(instanceFeatures[i]));
				}
				printFeatures = true;
			} 
			System.out.println("Instance Features Hash: " + ArrayMathOps.matlabHashCode(instanceFeatures));
			System.out.println("Param Values Hash:" + ArrayMathOps.matlabHashCode(paramValues));
			System.out.println("Used Instance IDs:" + Arrays.toString(usedInstancesIdxs));
			System.out.println("Num PCA:" + numPCA);
			System.out.println("Response Values:" +Arrays.toString(responseValues));
			System.out.println("Log Model: " + logModel);
		
		}
		*/
		instanceFeatures = ArrayMathOps.copy(instanceFeatures);
		writeOutput = false;
		if(writeOutput)
		{
			File f = new File(filename + "-" + index);
			f.delete();
			
			try { 
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f));
			/*
			System.out.println("double[][] instanceFeatures = " + explode(Arrays.deepToString(instanceFeatures)) + ";");
			System.out.println("double[][] paramValues = " + explode(Arrays.deepToString(paramValues)) + ";");
			System.out.println("double[] responseValues = " + explode(Arrays.toString(responseValues)) + ";");
			*/
			o.writeObject(instanceFeatures);
			o.writeObject(paramValues);
			o.writeObject(responseValues);
			o.writeObject(usedInstancesIdxs);
			System.out.println("Calls written & deleted to: " + filename + "-" + index++ );
			o.close();
			} catch(IOException e)
			{
				System.err.println(e);
			}
		}
		
		MessyMathHelperClass pca = new MessyMathHelperClass();
		double[][] usedInstanceFeatures = new double[usedInstancesIdxs.length][];
		
		for(int i=0; i < usedInstanceFeatures.length; i++)
		{
			usedInstanceFeatures[i] = instanceFeatures[usedInstancesIdxs[i]];
		}
		
	
		int[] constFeatures = pca.constantColumnsWithMissingValues(usedInstanceFeatures);
		instanceFeatures = pca.removeColumns(instanceFeatures, constFeatures);
		
		/*
		log.warn("Temporarily changed for debugging: PCAed only used instances");
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			int oldIndex = theta_inst_idxs[i][1];
			
			for(int j=0; j < usedInstancesIdxs.length; j++)
			{
				if(oldIndex == (usedInstancesIdxs[j]+1))
				{
					theta_inst_idxs[i][1] = j+1;
				}
			}
		}
		
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			int oldIndex = theta_inst_idxs[i][1];
			
			if(oldIndex > usedInstancesIdxs.length)
			{
				throw new IllegalStateException("Couldn't map instance");
			}
		}
		
		
		instanceFeatures = pca.removeColumns(usedInstanceFeatures, constFeatures);
		*/
		
		
		
		log.info("Discarding {} constant inputs of {} in total.", constFeatures.length, prePCAInstanceFeatures[0].length);
	/*
		if(RoundingMode.ROUND_NUMBERS_FOR_MATLAB_SYNC)
		{
			System.out.print("Constant Columns: ");
			for(int i=0; i < constFeatures.length; i++)
			{
				System.out.print(constFeatures[i]+1 + ",");
			}
			
			System.out.println("\n");
			System.out.println("Discarding "+ constFeatures.length + "  constant inputs of " + prePCAInstanceFeatures[0].length +" total ");
		}
		*/
		double[][] instanceFeaturesT = pca.transpose(instanceFeatures);
		
		
		double[] firstStdDev = pca.getRowStdDev(instanceFeaturesT);
		//double[][] pcaedFeatures =pca.getPCA(instanceFeatures, numPCA); 
		
		this.logModel = logModel;
		if(logModel)
		{
			pca.max(responseValues, SanitizedModelData.MINIMUM_RESPONSE_VALUE);
			pca.log10(responseValues);
		
		}
		
		//TODO: Give this variable an intellegent name
		int[] mySub = pca.getSub(firstStdDev);

		if(mySub.length == 0)
		{
			//throw new IllegalStateException("Not sure what to do in this case at the moment");
			sub = new int[0];
			means = new double[0];
			stdDev = new double[0];
			pcaCoeff = new double[0];
			pcaVec = new double[0][];
			pcaFeatures = new double[instanceFeatures.length][1];
			
			return;
		} else if (instanceFeatures[0].length < numPCA)
		{
			sub = new int[0];
			means = new double[0];
			stdDev = new double[0];
			pcaCoeff = new double[0];
			pcaVec = new double[0][];
			pcaFeatures = instanceFeatures;
			return;
		} else
		{
			sub = mySub;
		}
		instanceFeatures = pca.keepColumns(instanceFeatures, sub);
		instanceFeaturesT = pca.transpose(instanceFeatures);
		means = pca.getRowMeans(instanceFeaturesT);
		stdDev = pca.getRowStdDev(instanceFeaturesT);
		
				
		pca.perColumnOperation(instanceFeatures, means, Operation.SUBTRACT);
		pca.perColumnOperation(instanceFeatures, stdDev, Operation.DIVIDE);
		
		pcaCoeff = pca.getPCACoeff(instanceFeatures, numPCA);
		pcaVec = pca.getPCA(instanceFeatures, numPCA);
		
		
		//double[][] pcaVecT = pca.transpose(pcaVec);
		pcaFeatures = pca.matrixMultiply(instanceFeatures, pcaVec);
		
		/*
		if(RoundingMode.ROUND_NUMBERS_FOR_MATLAB_SYNC)
		{
			System.out.println("PCA Features Hash: " + ArrayMathOps.matlabHashCode(pcaFeatures));
		}
		*/
		
	}

	
	@Override
	public double[][] getPrePCAInstanceFeatures()
	{
		return prePCAInstanceFeatures;
	}
	
	@Override
	public double[][] getPCAVectors() {
		return pcaVec;
	}

	
	@Override
	public double[] getPCACoefficients() {
		return pcaCoeff;
	}

	
	@Override
	public int[] getDataRichIndexes() {
		return sub;
	}

	@Override
	public double[] getMeans() {
		return means;
	}

	
	@Override
	public double[] getStdDev() {
		return stdDev;
	}
	
	
	@Override
	public double[][] getPCAFeatures()
	{
		return pcaFeatures;
	}
	
	@Override
	public double[][] getConfigs()
	{
		return configs;
	}

	@Override
	public double[] getResponseValues()
	{
		return responseValues;
	}

	@Override
	public int[] getCategoricalSize()
	{
		return configSpace.getCategoricalSize();
	}
	
	@Override
	public int[][] getCondParents()
	{
		return configSpace.getCondParentsArray();
	}

	@Override
	public int[][][] getCondParentVals()
	{
		return configSpace.getCondParentValsArray();
	}

	@Override
	public double transformResponseValue(double d) {
		if(logModel)
		{
			
			return Math.log10(Math.max(d, SanitizedModelData.MINIMUM_RESPONSE_VALUE));
		} else
		{
			return d;
		}
	}

	@Override
	public int[][] getThetaInstIdxs() {

		int[][] theta_inst_idxs = new int[this.theta_inst_idxs.length][0];
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i] = this.theta_inst_idxs[i].clone();
		}
				
		return theta_inst_idxs; 
	}

	@Override
	public boolean[] getCensoredResponses() {
		return this.censoredResponseValues;
	}
	
}

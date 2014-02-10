package ca.ubc.cs.beta.aclib.acquisitionfunctions;


import static ca.ubc.cs.beta.aclib.misc.math.ArrayMathOps.*;

public class LowerConfidenceBound implements AcquisitionFunction {
	
	
	public LowerConfidenceBound()
	{
		
	}
	
	@Override
	public double[] computeAcquisitionFunctionValue(double k,
			double[] predmean, double[] predvar) {
			if(predmean.length != predvar.length)
			{
				throw new IllegalArgumentException("Expected predmean and predvar to have the same length");
			}
			return	add( predmean,	times(-k,sqrt(predvar)));
	}


}

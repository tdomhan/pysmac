package ca.ubc.cs.beta.aclib.misc.debug;

public final class DebugUtil {
	public static String getCurrentMethodName()
	{
		
		Exception e = new Exception();
		try {
			String methodName =  e.getStackTrace()[1].getMethodName();
			System.err.println("Method Name Returned:" + methodName);
			return methodName;
		} catch(RuntimeException e2)
		{
			
			return "Unknown Method";
		}
	}
	
	private DebugUtil()
	{
		
	}
}

package ca.ubc.cs.beta.aclib.configspace;

public class ParamConfigurationStringFormatException extends RuntimeException {

	public ParamConfigurationStringFormatException(String string,
			RuntimeException e) {
		super(string, e);
	}

	public ParamConfigurationStringFormatException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8666575841906423444L;

}

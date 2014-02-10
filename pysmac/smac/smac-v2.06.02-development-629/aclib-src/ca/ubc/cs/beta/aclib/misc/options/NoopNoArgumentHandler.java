package ca.ubc.cs.beta.aclib.misc.options;

public class NoopNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		return false;
	}

}

package ca.ubc.cs.beta.aclib.smac;

/**
 * Specifies what mode the Automatic Configuration should execute
 * 
 * 
 */
public enum ExecutionMode {
	
	/**
	 * Standard SMAC mode
	 */
	SMAC,
	
	/**
	 * ROAR only mode (no model, use random configurations)
	 */
	ROAR,
	
	
	/**
	 * Point Selector Mode
	 */
	PSEL
	
}

package ca.ubc.cs.beta.aclib.algorithmrun.kill;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;

/**
 * Represents an Algorithm Run as it existed at some point of time 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface KillableAlgorithmRun extends AlgorithmRun {

	public void kill();
}

package ca.ubc.cs.beta.aclib.runhistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;


/**
 * TeeRunHistory is a RunHistory object that on top of notifying the decorated RunHistory object, also notifies another one but otherwise acts as a transparent decorator.
 * <br>
 * <b>Note:</b>Duplicate runs in the branch are simply silenced.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class TeeRunHistory extends AbstractRunHistoryDecorator{

	private RunHistory branch;

	private Logger log = LoggerFactory.getLogger(this.getClass());
	public TeeRunHistory(RunHistory out, RunHistory branch) {
		super(out);
		this.branch = branch;
	}

	@Override
	public void append(AlgorithmRun run) throws DuplicateRunException {
		rh.append(run);
		
		try {
			branch.append(run);
		} catch(DuplicateRunException e)
		{
			log.debug("Branch RunHistory object detected duplicate run: {}", run);
		}
	}

	@Override
	public int getOrCreateThetaIdx(ParamConfiguration initialIncumbent) {
		
		try {
		return this.rh.getOrCreateThetaIdx(initialIncumbent);
		} finally
		{
			this.branch.getOrCreateThetaIdx(initialIncumbent);
		}
	}
	
	
}

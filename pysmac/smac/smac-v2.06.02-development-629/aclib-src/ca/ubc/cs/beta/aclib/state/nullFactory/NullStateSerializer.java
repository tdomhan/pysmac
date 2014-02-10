package ca.ubc.cs.beta.aclib.state.nullFactory;

import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
import ca.ubc.cs.beta.aclib.state.StateSerializer;

public class NullStateSerializer implements StateSerializer{
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void setRunHistory(RunHistory runHistory) {

		
	}

	@Override
	public void save() {
		log.trace("Null State Serializer Selected, no data saved");
		
	}

	@Override
	public void setIncumbent(ParamConfiguration config) {
		
	}



	@Override
	public void setObjectStateMap(Map<String, Serializable> objectState) {
		
	}

}

package ca.ubc.cs.beta.aclib.configspace.tracking;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;

public class NullParamConfigurationOriginTracker implements
		ParamConfigurationOriginTracker {

	@Override
	public void addConfiguration(ParamConfiguration config, String origin,
			String... addlData) {
		
	}

	@Override
	public Map<String, String> getOrigins(ParamConfiguration config) {
		return Collections.emptyMap();
	}

	@Override
	public Long getCreationTime(ParamConfiguration config) {

		return 0L;
	}

	@Override
	public Set<String> getOriginNames() {
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<ParamConfiguration> iterator() {

		return Collections.EMPTY_LIST.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int getGenerationCount(ParamConfiguration config) {
		return 0;
	}

}

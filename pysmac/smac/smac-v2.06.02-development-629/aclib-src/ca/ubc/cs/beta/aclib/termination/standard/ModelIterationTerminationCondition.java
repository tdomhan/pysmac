package ca.ubc.cs.beta.aclib.termination.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildEndEvent;
import ca.ubc.cs.beta.aclib.termination.ConditionType;
import ca.ubc.cs.beta.aclib.termination.ValueMaxStatus;

@ThreadSafe
public class ModelIterationTerminationCondition extends AbstractTerminationCondition implements EventHandler<ModelBuildEndEvent> {

	private final String NAME = "NUMBER OF RUNS";
	private final  long modelBuildLimit;
	private final AtomicLong modelBuildIteration = new AtomicLong(0);

	public ModelIterationTerminationCondition(long modelBuildLimit)
	{
		this.modelBuildLimit = modelBuildLimit;
	}
		

	@Override
	public boolean haveToStop() {
		return (modelBuildIteration.get() >= modelBuildLimit);
			
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.singleton(new ValueMaxStatus(ConditionType.OTHER, modelBuildIteration.get(), modelBuildLimit, NAME, "Model/Iteration", ""));
	}

	@Override
	public void handleEvent(ModelBuildEndEvent event) {
		modelBuildIteration.incrementAndGet();
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public void registerWithEventManager(EventManager evtManager) {
		evtManager.registerHandler(ModelBuildEndEvent.class, this);
	}


	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "Model Building / Iteration Limit (" +  modelBuildIteration +  ") has been reached";
		} else
		{
			return "";
		}
		
	}
	
}

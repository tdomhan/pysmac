package ca.ubc.cs.beta.aclib.termination.standard;

import java.util.Collection;
import java.util.Collections;
import com.google.common.util.concurrent.AtomicDouble;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.termination.ConditionType;
import ca.ubc.cs.beta.aclib.termination.ValueMaxStatus;
import static ca.ubc.cs.beta.aclib.misc.cputime.CPUTime.*;

@ThreadSafe
public class CPULimitCondition extends AbstractTerminationCondition 
{

	private final double tunerTimeLimit;
	private AtomicDouble currentTime;
	
	private final String NAME = "CPUTIME";
	private final boolean countACTime;

	//private final AtomicBoolean haveToStop = new AtomicBoolean(false);
	public CPULimitCondition(double tunerTimeLimit, boolean countACTime)
	{
		this.tunerTimeLimit = tunerTimeLimit;
		this.currentTime = new AtomicDouble(0);
		this.countACTime = countACTime;
	}
	
	public double getTunerTime()
	{
		
		return currentTime.get() + ((countACTime) ? getCPUTime() : 0);
	}

	
	@Override
	public boolean haveToStop() {
		return (tunerTimeLimit <= getTunerTime());
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		double tunerTime = getTunerTime();
		
		return Collections.singleton(new ValueMaxStatus(ConditionType.TUNERTIME, tunerTime, tunerTimeLimit, NAME, "Configuration Time Budget", "s"));
	}

	@Override
	public synchronized void notifyRun(AlgorithmRun run) {
		currentTime.addAndGet(Math.max(0.1, run.getRuntime()));
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "Tuner Time Limit (" +  tunerTimeLimit +  " s) has been reached";
		} else
		{
			return "";
		}
		
	}

	
}

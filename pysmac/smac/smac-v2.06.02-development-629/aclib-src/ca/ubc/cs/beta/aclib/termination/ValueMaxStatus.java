package ca.ubc.cs.beta.aclib.termination;

/**
 * A tuple object that stores information about the current state 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class ValueMaxStatus {

	private final double current;
	private final double max;
	private final String status;
	private final String name;
	private ConditionType type;

	public ValueMaxStatus(ConditionType type, double current, double max, String name, String status)
	{
		this.type = type;
		this.current = current;
		this.max = max;
		this.status  = status;
		this.name = name;
	}
	
	public ValueMaxStatus(ConditionType type, double current, double max, String name)
	{
		this.type = type;
		this.current = current;
		this.max = max;
		this.name = name;
		
		this.status = name + " is currently (" +current +  "). Max is ( " + max + " ) "; 
	}

	
	public ValueMaxStatus(ConditionType type, double current, double max, String name, String friendlyName, String unit)
	{
		this.type = type;
		this.current = current;
		this.max = max;
		this.name = name;
		this.status = friendlyName + " used: " + current + " "+unit+ " (" + ((int)( current / max * 100)) + "%)\n" + friendlyName + " remaining: " + (max - current) + " " + unit + "\n";
		
		//this.status = name + " is currently (" +current +  "). Max is ( " + max + " ) "; 
	}
	
	public double getCurrent() {
		return current;
	}

	public double getMax() {
		return max;
	}

	public String getName()
	{
		return name;
		
	}
	public String getStatus() {
		return status;
	}
	
	public String toString()
	{
		return getStatus();
	}
	
	public ConditionType getType()
	{
		return type;
	}
	
	
}


package ca.ubc.cs.beta.aclib.misc.associatedvalue;

/**
 * The Pair class is an immutable pairing of two objects
 * 
 * @author geschd
 *
 * @param <T>
 * @param <V>
 */
public class Pair<T,V> {
	
	private final T Tobj;
	private final V Vobj;
	/**
	 * Standard Constructor
	 * @param t Value
	 * @param v Value 
	 */
	public Pair(T t, V v)
	{
		Tobj = t;
		Vobj = v;
	}

	/**
	 * Retrieves the first object this object
	 * @return associated value
	 */
	public T getFirst()
	{
		return Tobj;
	}
	/**
	 * Retrieves the second object in the pair
	 * @return second object
	 */
	public V getSecond()
	{
		return Vobj;
	}
	
	@Override
	public String toString()
	{
		return "<"+Tobj.toString() + ", " + Vobj.toString()+">";
	}
	
	@Override
	/**
	 * Two pairs are the same if the two contained objects are the same.
	 */
	public boolean equals(Object o)
	{
		if(o instanceof Pair<?,?>)
		{
			Pair<?,?> oValue = (Pair<?, ?>) o;
			return (Tobj.equals(oValue.Tobj) && Vobj.equals(oValue.Vobj));
		} else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return Vobj.hashCode() ^ Tobj.hashCode();
	}


}

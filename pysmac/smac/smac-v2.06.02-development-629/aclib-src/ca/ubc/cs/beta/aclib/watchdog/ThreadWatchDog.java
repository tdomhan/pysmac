package ca.ubc.cs.beta.aclib.watchdog;

import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
import ca.ubc.cs.beta.aclib.eventsystem.events.AutomaticConfiguratorEvent;

public interface ThreadWatchDog<K extends AutomaticConfiguratorEvent> extends EventHandler<K>{

	public void handleEvent(K event);

	public void registerCurrentThread();

	public void registerThread(Thread t);

}
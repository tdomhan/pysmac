package ca.ubc.cs.beta.aclib.watchdog;

import ca.ubc.cs.beta.aclib.eventsystem.events.AutomaticConfiguratorEvent;

public class NullThreadWatchDog<K extends AutomaticConfiguratorEvent> implements ThreadWatchDog<K> {

	@Override
	public void handleEvent(K event) {

		
	}

	@Override
	public void registerCurrentThread() {

		
	}

	@Override
	public void registerThread(Thread t) {

		
	}


}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.debug;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Detects an unclean shutdown, shutdowns where notify shutdown was not called on the TAE.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class UncleanShutdownDetectingTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator 
		{

	private final AtomicLong notifyShutdownInvoked = new AtomicLong(0);
	private static final int MESSAGE_REPEAT = 1;
	private static final int SLEEP_TIME_IN_MS = 0;
	private static final int SLEEP_TIME_BETWEEN_MESSAGES = 0;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final StackTraceElement[] taeCreationStackTrace;
	
	private final static Object stackTracePrintingLock = new Object();

	public UncleanShutdownDetectingTargetAlgorithmEvaluator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
		taeCreationStackTrace = (new Exception()).getStackTrace();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{

			@Override
			public void run() {
				Thread.currentThread().setName("Unclean Target Algorithm Evaluator Shutdown Detector");
				try {
					long notifies = notifyShutdownInvoked.get();
					if(notifies == 0)
					{
						log.error("Unclean Shutdown Detected, You must call notifyShutdown() on your TAE. You may have a broken TAE decorator that doesn't forward the notifyShutdown() correctly");
						for(int i=0; i < MESSAGE_REPEAT; i++)
						{
							System.err.println("Arrêt anormal détecté. Vous devez appeler notifyShutdown() de votre TAE. Vous pouvez avoir un TAE qui bloque la transmission appropriée.\n Unclean Shutdown Detected, You must call notifyShutdown() on your TAE before exiting. You may have a decorator that doesn't forward the call correctly.");
							Thread.sleep(SLEEP_TIME_BETWEEN_MESSAGES);
						}
						
						
						
						Thread.sleep(SLEEP_TIME_IN_MS);
					} else if(notifies > 1)
					{
						log.warn("You called notifyShutdown() on your TAE more than once, this seems exceptionally weird");
						
						for(int i=0; i < MESSAGE_REPEAT; i++)
						{
							System.err.println("You called notifyShutdown() on your TAE more than once, this is almost certainly a logic error and may cause weird behaivour");
							Thread.sleep(SLEEP_TIME_BETWEEN_MESSAGES);
						}
						Thread.sleep(SLEEP_TIME_IN_MS);
					} else if(notifies < 0)
					{
						log.warn("You seem to have overflowed the counter we use to track the number of calls to notifyShutdown(), well played...");
						
						for(int i=0; i < MESSAGE_REPEAT; i++)
						{
							System.err.println("You seem to have overflowed the counter we use to notify shutdown. I don't even have words for this");
							Thread.sleep(SLEEP_TIME_BETWEEN_MESSAGES);
						}
						Thread.sleep(SLEEP_TIME_IN_MS);
					} else
					{
						//Yay they cleaned up properly
						return;
					}
					
					synchronized(stackTracePrintingLock)
					{
						if(log.isDebugEnabled())
						{
							System.err.println("Target Algorithm Evaluator that wasn't shutdown, was created here");
							for(StackTraceElement el : taeCreationStackTrace)
							{
								System.err.println(el);
							}
						}
					}
					
				} catch(InterruptedException e)
				{
					System.err.println("Interrupted while trying to make you wait, don't think you are off the hook");
					Thread.currentThread().interrupt();
					return;
				}
				
				
				
			}
			
		}
		));
		
	}

	@Override
	public void notifyShutdown()
	{
		notifyShutdownInvoked.incrementAndGet();
		tae.notifyShutdown();
	}
}

package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aclib.algorithmrun.AbstractAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.RunningAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillHandler;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableWrappedAlgorithmRun;
import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.logback.MarkerFilter;
import ca.ubc.cs.beta.aclib.misc.logging.LoggingMarker;
import ca.ubc.cs.beta.aclib.misc.string.SplitQuotedString;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Executes a Target Algorithm Run via Command Line Execution
 * @author sjr
 *
 */
public class CommandLineAlgorithmRun extends AbstractAlgorithmRun {

	
	private static final long serialVersionUID = -70897405824987641L;
	
	/**
	 * Regex that we hope to match
	 */
	public static final String AUTOMATIC_CONFIGURATOR_RESULT_REGEX = "^\\s*(Final)?\\s*[Rr]esult\\s+(?:([Ff]or)|([oO]f))\\s+(?:(HAL)|(ParamILS)|(SMAC)|([tT]his [wW]rapper)):";
	
	/**
	 * Compiled REGEX
	 */
	private static final Pattern pattern = Pattern.compile(AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
	
	private static transient Logger log = LoggerFactory.getLogger(CommandLineAlgorithmRun.class);
	
	
	private Queue<String> outputQueue = new ArrayDeque<String>(MAX_LINES_TO_SAVE * 2);

	/**
	 * Stores the observer for this run
	 */
	private transient TargetAlgorithmEvaluatorRunObserver runObserver;

	/**
	 * Stores the kill handler for this run
	 */
	private transient KillHandler killHandler;
	
	public static final String PORT_ENVIRONMENT_VARIABLE = "ACLIB_PORT";
	public static final String FREQUENCY_ENVIRONMENT_VARIABLE = "ACLIB_CPU_TIME_FREQUENCY";
	public static final String CONCURRENT_TASK_ID = "ACLIB_CONCURRENT_TASK_ID";
	
	/**
	 * Marker for logging
	 */
	private static transient Marker fullProcessOutputMarker = MarkerFactory.getMarker(LoggingMarker.FULL_PROCESS_OUTPUT.name());
	
	private static String commandSeparator = ";";
	
	
	
	static {
		log.warn("This version of SMAC hardcodes run length for calls to the target algorithm to {}.", Integer.MAX_VALUE);
		
		if(System.getProperty("os.name").toLowerCase().contains("win"))
		{
			commandSeparator = "&";
		}
		
	}
	
	private static final double WALLCLOCK_TIMING_SLACK = 0.001;
	
	private transient ExecutorService threadPoolExecutor = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Command Line Target Algorithm Evaluator Thread ")); 
	
	private final int observerFrequency;
	
	private AtomicBoolean processEnded = new AtomicBoolean(false);
	
	private transient final BlockingQueue<Integer> executionIDs;
	
	
	/**
	 * This field is transient because we can't save this object when we serialize.
	 * 
	 * If after restoring serialization you need something from this object, you should
	 * save it as a separate field. (this seems unlikely) 
	 * 
	 */
	private final transient CommandLineTargetAlgorithmEvaluatorOptions options;
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param executionIDs 
	 */
	public CommandLineAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig, TargetAlgorithmEvaluatorRunObserver obs, KillHandler handler, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs) 
	{
		super(execConfig, runConfig);
		//TODO Test
		if(runConfig.getCutoffTime() <= 0 || handler.isKilled())
		{
			
			log.info("Cap time is less than or equal to zero for {} setting run as timeout", runConfig);
			String rawResultLine = "[DIDN'T BOTHER TO RUN ALGORITHM AS THE CAPTIME IS NOT POSITIVE]";
			
			this.setResult(RunResult.TIMEOUT, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine,"");
		}
		
		this.runObserver = obs;
		this.killHandler = handler;
		this.observerFrequency = options.observerFrequency;
		
		if(observerFrequency < 25)
		{
			throw new IllegalArgumentException("Observer Frequency can't be less than 25 milliseconds");
		}
		
		this.options = options;
		this.executionIDs = executionIDs;
	}
	
	private static final int MAX_LINES_TO_SAVE = 1000;

	private volatile boolean wasKilled = false;
	
	@Override
	public synchronized void run() {
		
		if(this.isRunCompleted())
		{
			return;
		}
		
		//Notify observer first to trigger kill handler
		runObserver.currentStatus(Collections.singletonList((KillableAlgorithmRun) new RunningAlgorithmRun(execConfig, getRunConfig(),  0,  0,0, getRunConfig().getProblemInstanceSeedPair().getSeed(), 0, killHandler)));
		
		if(killHandler.isKilled())
		{
			
			log.debug("Run was killed", runConfig);
			String rawResultLine = "Killed Manually";
			
			this.setResult(RunResult.KILLED, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine,"");
			return;
		}
		
		final Process proc;
		
		
		
		try 
		{
			Integer token;
			try {
				 token = executionIDs.take();
			} catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				this.setResult(RunResult.ABORT, 0, 0, 0, 0, "", "Target CLI Thread was Interrupted");
				return;
			}
			 
			
			try 
			{
				//Check kill handler again
				if(killHandler.isKilled())
				{
					
					log.debug("Run was killed", runConfig);
					String rawResultLine = "Killed Manually";
					
					this.setResult(RunResult.KILLED, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine,"");
					return;
				}
				
				
				final AtomicInteger pid = new AtomicInteger(-1);
				int port = 0;
				final DatagramSocket serverSocket;
				if(options.listenForUpdates)
				{
					serverSocket = new DatagramSocket();
					port = serverSocket.getLocalPort();
				} else
				{
					serverSocket = null;
				}
				
				final AtomicDouble currentRuntime = new AtomicDouble(0);
				
				Runnable socketThread = new Runnable()
				{
					@Override
					public void run()
					{
						
						byte[] receiveData = new byte[1024];
						
						while(true)
						{
							try 
							{
								DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
								
								
								serverSocket.receive(receivePacket);
								
								InetAddress IPAddress = receivePacket.getAddress();
					               
								if (!InetAddress.getByName("localhost").equals(IPAddress))
								{
									log.warn("Received Request from Non-localhost, ignoring request from: {}", IPAddress.getHostAddress());
									continue;
								}
				               
				               Double runtime = Double.valueOf(new String(receivePacket.getData()));
				               
				               currentRuntime.set(runtime);
				               
							} catch(RuntimeException e)
							{
								log.debug("Got some runtime exception while processing data packet", e);
							} catch(SocketException e)
							{
								log.trace("SocketException occurred which we expected when we shutdown probably", e);
								return;
							} catch (IOException e) {
								log.warn("Unknown IOException occurred ", e);
							}
							
						}
					}
					
					
				};
				
				this.startWallclockTimer();
				proc = runProcess(port, token);
			
				
				
				
				final Process innerProcess = proc; 
				
				final Semaphore stdErrorDone = new Semaphore(0);
			
				Runnable standardErrorReader = new Runnable()
				{
	
					@Override
					public void run() {
						
						Thread.currentThread().setName("Command Line Target Algorithm Evaluator Thread (Standard Error Processor)" + getRunConfig() );
						try {
							try { 
								BufferedReader procIn = new BufferedReader(new InputStreamReader(innerProcess.getErrorStream()));
								
							
								do{
									
									String line;
									boolean read = false;
									while(procIn.ready())
									{
										read = true;
										line = procIn.readLine();
										
										if(line == null)
										{
											
											return;
										}
										log.warn("[PROCESS-ERR]  {}", line);
										
									}
								
									
									if(!read)
									{
										Thread.sleep(50);
									}
									
								} while(!processEnded.get());
								
								
								StringBuilder sb = new StringBuilder();
								
								//In case something else has come in
								if(procIn.ready())
								{
									//Probably not the most efficient way to read
									char[] input = new char[10000];
									procIn.read(input);
									sb.append(String.valueOf(input));
									
								}
								
								if(sb.toString().trim().length() > 0)
								{
									log.warn("[PROCESS-ERR] {}", sb.toString().trim());
								}
							
								procIn.close();
							} finally
							{
								
								stdErrorDone.release();
								log.debug("Standard Error Done");
							}
						} catch(InterruptedException e)
						{
							Thread.currentThread().interrupt();
							return;
						} catch(IOException e)
						{
							log.warn("Unexpected IOException occurred {}",e);
						}
						
						
					}
					
				};
				
				
				
				
			
				
				Runnable observerThread = new Runnable()
				{
	
					@Override
					public void run() {
						Thread.currentThread().setName("Command Line Target Algorithm Evaluator Thread (Observer)" + getRunConfig());
	
						while(true)
						{
						
							double currentTime = getCurrentWallClockTime() / 1000.0;
							
							runObserver.currentStatus(Collections.singletonList((KillableAlgorithmRun) new RunningAlgorithmRun(execConfig, getRunConfig(),  Math.max(0,currentRuntime.get()),  0,0, getRunConfig().getProblemInstanceSeedPair().getSeed(), currentTime, killHandler)));
							try {
								
								
								
								//Sleep here so that maybe anything that wanted us dead will have gotten to the killHandler
								Thread.sleep(25);
								if(killHandler.isKilled())
								{
									wasKilled = true;
									log.debug("Trying to kill");
									killProcess(proc);
									//log.debug("Process destroy() called now waiting for completion");
									
									//log.debug("Process has exited with code {}", proc.waitFor());
									return;
								}
								Thread.sleep(observerFrequency - 25);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								break;
							}
							
						}
						
						
					}
					
				};
				
				
				if(options.listenForUpdates)
				{
					threadPoolExecutor.execute(socketThread);
				}
				threadPoolExecutor.execute(observerThread);
				threadPoolExecutor.execute(standardErrorReader);
				BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				
				//Scanner procIn = new Scanner(proc.getInputStream());
			
				processRunLoop(read,proc);
				
				killProcess(proc);
				
				if(!this.isRunCompleted())
				{
					if(wasKilled)
					{
						double currentTime = Math.max(0,currentRuntime.get());
						this.setResult(RunResult.KILLED, currentTime, 0,0, getRunConfig().getProblemInstanceSeedPair().getSeed(), "Killed Manually", "" );
						
					} else {
						this.setCrashResult("Wrapper did not output anything that matched our regex please see the manual for more information. Please try executing the wrapper directly and ensuring that some line starts with: \"Results for ParamILS:\" (case sensitive). In more gorey detail it needs to match the following Regular Expression: " + AUTOMATIC_CONFIGURATOR_RESULT_REGEX );
					}
				}
				
				
				switch(this.getRunResult())
				{
				
				
				case ABORT:
				case CRASHED:
					
				
						log.error( "Failed Run Detected Call: cd \"{}\" " + commandSeparator + "  {} ",new File(execConfig.getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig));
					
						log.error("Failed Run Detected output last {} lines", outputQueue.size());
						
						for(String s : outputQueue)
						{
							log.error("> "+s);
						}
						log.error("Output complete");
						
					
				default:
					
				}
				
				outputQueue.clear();
				
				
				read.close();
			
				
				
				
				stdErrorDone.acquireUninterruptibly();
				
				threadPoolExecutor.shutdownNow();
				//Close the listening socket
				serverSocket.close();
				try {
					threadPoolExecutor.awaitTermination(24, TimeUnit.HOURS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
	
				runObserver.currentStatus(Collections.singletonList(new KillableWrappedAlgorithmRun(this)));
				log.debug("Run {} is completed", this);
			} finally
			{
				if(!executionIDs.offer(token))
				{
					log.error("Developer Error: Couldn't offer run token back to pool, which violates an invariant. We will essentially block until it is accepted.");
					try {
						executionIDs.put(token);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				
				
			}
		} catch (IOException e1) {
			//String execCmd = getTargetAlgorithmExecutionCommandAsString(execConfig,runConfig);
			log.error( "Failed Run Detected (IOException) Call: cd \"{}\" " + commandSeparator + "  {} ",new File(execConfig.getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig));
			throw new TargetAlgorithmAbortException(e1);
			//throw new IllegalStateException(e1);
		}
		
		
		
		
	}
	
	/**
	 * Processes all the output of the target algorithm
	 * 
	 * Takes a line from the input and tries to parse it 
	 * 
	 * @param procIn Scanner of processes output stream
	 */
	public void processRunLoop(BufferedReader procIn, Process p)
	{
		
		int i=0; 
			try {
				boolean matchFound = false;
outerloop:
				do{
				
					String line;
					boolean read = false;
					//TODO This ready call doesn't guarantee we can read a line
					
					while(procIn.ready())
					{
						read = true;
						line = procIn.readLine();
						
						if(line == null)
						{
							log.debug("Process has ended");
							processEnded.set(true);
							break outerloop;
						}
						outputQueue.add(line);
						if (outputQueue.size() > MAX_LINES_TO_SAVE)
						{
							outputQueue.poll();
						}
						
					
						
						if(wasKilled)
						{
							continue;
						}
						boolean matched = processLine(line);
						
						
						if(matched && matchFound)
						{
							log.error("Second output of matching line detected, there is a problem with your wrapper. You can try turning with log all process output enabled to debug: {} ", line);
							this.setAbortResult("duplicate lines matched");
							continue;
						}
						matchFound = matchFound | matched; 
						
						
								
						
					}
					
					
					if(this.isRunCompleted() && wasKilled)
					{
						log.warn("Run was killed but we somehow completed this might be a race condition but our result is: {}. This is a warning just so that developers can see this having occurred and judge the correctness" , this.getResultLine());
					}
					
					
					
					if(!procIn.ready() && exited(p))
					{
						//I assume that if the stream isn't ready and the process has exited that 
						//we have processed everything
						processEnded.set(true);
						break;
					}
					
					if(!read)
					{
						if(++i % 200 == 0)
						{
							log.debug("Slept for 5 second waiting for pid {}  && {} " ,getPID(p), matchFound);
						}
						Thread.sleep(25);
					}
					
				} while(!processEnded.get());
				
				procIn.close();
			} catch (IOException e) {
				
				if(!processEnded.get())
				{
					log.debug("IO Exception occurred while processing runs");
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			
			}
		
		
	}
	
	

	/**
	 * Starts the target algorithm
	 * @param token 
	 * @return Process reference to the executiong process
	 * @throws IOException
	 */
	private  Process runProcess(int port, Integer token) throws IOException
	{
		String[] execCmdArray = getTargetAlgorithmExecutionCommand(execConfig, runConfig);
		
		
		if(options.logAllCallStrings)
		{
			log.info( "Call (with token {}) : cd \"{}\" " + commandSeparator + "  {} ", token, new File(execConfig.getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig));
		}
		
		
		ArrayList<String> envpList = new ArrayList<String>(System.getenv().size());
		for(Entry<String, String> ent : System.getenv().entrySet())
		{
			envpList.add(ent.getKey() + "=" + ent.getValue());
		}
		
		if(options.listenForUpdates)
		{
			envpList.add(PORT_ENVIRONMENT_VARIABLE  + "=" + port);
			envpList.add(FREQUENCY_ENVIRONMENT_VARIABLE + "=" + (this.observerFrequency / 2000.0));
			
		}
		
		envpList.add(CONCURRENT_TASK_ID + "=" + token);
		
		String[] envp = envpList.toArray(new String[0]);
		Process proc = Runtime.getRuntime().exec(execCmdArray,envp, new File(execConfig.getAlgorithmExecutionDirectory()));
		return proc;
	}
	
	
	/**
	 * Gets the execution command string
	 * @return string containing command
	 */
	private static String[] getTargetAlgorithmExecutionCommand(AlgorithmExecutionConfig execConfig, RunConfig runConfig)
	{

				
		String cmd = execConfig.getAlgorithmExecutable();
		cmd = cmd.replace(AlgorithmExecutionConfig.MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX,"");
		
		
		String[] execCmdArray = SplitQuotedString.splitQuotedString(cmd);
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(execCmdArray));
		list.add(runConfig.getProblemInstanceSeedPair().getInstance().getInstanceName());
		list.add(runConfig.getProblemInstanceSeedPair().getInstance().getInstanceSpecificInformation());
		list.add(String.valueOf(runConfig.getCutoffTime()));
		list.add(String.valueOf(Integer.MAX_VALUE));
		list.add(String.valueOf(runConfig.getProblemInstanceSeedPair().getSeed()));
		
		StringFormat f = StringFormat.NODB_SYNTAX;
		
		for(String key : runConfig.getParamConfiguration().getActiveParameters() )
		{
			
			
			if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
			{
				throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
			}
			list.add(f.getPreKey() + key);
			list.add(f.getValueDelimeter() + runConfig.getParamConfiguration().get(key)  + f.getValueDelimeter());	
			
		}
		
		
		//execString.append(cmd).append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append();
		
		return list.toArray(new String[0]);
	}
	
	/**
	 * Gets the execution command string
	 * @return string containing command
	 */
	public static String getTargetAlgorithmExecutionCommandAsString(AlgorithmExecutionConfig execConfig, RunConfig runConfig)
	{

				
		String cmd = execConfig.getAlgorithmExecutable();
		cmd = cmd.replace(AlgorithmExecutionConfig.MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX,"");
		
		
		String[] execCmdArray = SplitQuotedString.splitQuotedString(cmd);
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(execCmdArray));
		list.add(runConfig.getProblemInstanceSeedPair().getInstance().getInstanceName());
		list.add(runConfig.getProblemInstanceSeedPair().getInstance().getInstanceSpecificInformation());
		list.add(String.valueOf(runConfig.getCutoffTime()));
		list.add(String.valueOf(Integer.MAX_VALUE));
		list.add(String.valueOf(runConfig.getProblemInstanceSeedPair().getSeed()));
		
		StringFormat f = StringFormat.NODB_SYNTAX;
		for(String key : runConfig.getParamConfiguration().getActiveParameters()  )
		{
			
			
			if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
			{
				throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
			}
			list.add(f.getPreKey() + key);
			list.add(f.getValueDelimeter() + runConfig.getParamConfiguration().get(key)  + f.getValueDelimeter());	
			
		}
		
		
		StringBuilder sb = new StringBuilder();
		for(String s : list)
		{
			if(s.matches(".*\\s+.*"))
			{
				sb.append("\""+s + "\"");
			} else
			{
				sb.append(s);
			}
			sb.append(" ");
		}
		
		
		//execString.append(cmd).append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append();
		
		return sb.toString();
	}


	
	
	/**
	 *	Process a single line of the output looking for a matching line (e.g. Result for ParamILS: ...)
	 *	@param line of program output
	 */
	public boolean processLine(String line)
	{
		Matcher matcher = pattern.matcher(line);
		String rawResultLine = "[No Matching Output Found]";
		
		if(options.logAllProcessOutput)
		{
			log.debug("[PROCESS]  {}" ,line);
		}
		

		if (matcher.find())
		{
		
			String fullLine = line.trim();
			String additionalRunData = "";
			try
			{
			
				String acExecResultString = line.substring(matcher.end()).trim();
				
				String[] results = acExecResultString.split(",");
				for(int i=0; i < results.length; i++)
				{
					results[i] = results[i].trim();
				}
				
				rawResultLine = acExecResultString;
				
				RunResult acResult =  RunResult.getAutomaticConfiguratorResultForKey(results[0]);
				
				if(!acResult.permittedByWrappers())
				{
					throw new IllegalArgumentException(" The Run Result reported is NOT permitted to be output by a wrapper and is for internal SMAC use only.");
				}
				
					
					
				String runtime = results[1].trim();
				String runLength = results[2].trim();
				String bestSolution = results[3].trim();
				String seed = results[4].trim();
				if(results.length <= 5)
				{ //This is a good case

				} else if(results.length == 6)
				{
					additionalRunData = results[5].trim();
				} else
				{
					log.warn("Too many fields were encounted (expected 5 or 6) when parsing line (Additional Run Data cannot have commas): {}\n ",line);
				}
				
				double runLengthD = Double.valueOf(runLength);
				double runtimeD = Double.valueOf(runtime);
				double qualityD = Double.valueOf(bestSolution);
				long resultSeedD = Long.valueOf(seed);
				if(!MarkerFilter.log(fullProcessOutputMarker.getName()))
				{
					log.info("Algorithm Reported: {}" , line);
				}
				
				this.setResult(acResult, runtimeD, runLengthD, qualityD, resultSeedD, rawResultLine, additionalRunData);
				return true;
			} catch(NumberFormatException e)
			{	 //Numeric value is probably at fault
				this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				Object[] args = { getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely one of the values of runLength, runtime, quality could not be parsed as a Double, or the seed could not be parsed as a valid long", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunResult.CRASHED);
				return true;
					
			} catch(IllegalArgumentException e)
			{ 	//The RunResult probably doesn't match anything
				this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				
				
				ArrayList<String> validValues = new ArrayList<String>();
				for(RunResult r : RunResult.values())
				{
					if(r.permittedByWrappers())
					{
						validValues.addAll(r.getAliases());
					}
				}
				Collections.sort(validValues);
				
				String[] validArgs = validValues.toArray(new String[0]);
				
				
				Object[] args = { getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig), fullLine, Arrays.toString(validArgs)};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the Algorithm did not report a result string as one of: {}", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunResult.CRASHED);
				return true;
			} catch(ArrayIndexOutOfBoundsException e)
			{	//There aren't enough commas in the output
				this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				Object[] args = { getTargetAlgorithmExecutionCommandAsString(execConfig, runConfig), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the algorithm did not specify all of the required outputs that is <solved>,<runtime>,<runlength>,<quality>,<seed>", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunResult.CRASHED);
				return true;
			}
		}
		
		return false;
		
	}

	
	
	private static int getPID(Process p)
	{
		int pid = 0;
		
		try {
			Field f = p.getClass().getDeclaredField("pid");
			
			f.setAccessible(true);
			pid = Integer.valueOf(f.get(p).toString());
			f.setAccessible(false);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			return -1;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		if(pid > 0)
		{
			return pid;
		} else
		{
			return -1;
		}
	}
	
	public static boolean exited(Process p)
	{
		try 
		{
			p.exitValue();
			return true;
		} catch(IllegalThreadStateException e)
		{
			return false;
		}
	}
	
	private String replacePid(String input, int pid)
	{
		return input.replaceAll("%pid", String.valueOf(pid));
	}
	private void killProcess(Process p)
	{
		
		try 
		{
			if(exited(p))
			{
				return;
			}
		
			try 
			{
				int pid = getPID(p);
				try
				{
					
					if(pid > 0)
					{
						
						
						
						String command = replacePid(options.pgNiceKillCommand,pid);
						log.debug("Trying to send SIGTERM to process group id: {} with command \"{}\"", pid,command);
						try {
							
							Process p2 = Runtime.getRuntime().exec(SplitQuotedString.splitQuotedString(command));
							
							BufferedReader read = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
							String line = null;
							
							while((line = read.readLine()) != null)
							{
								log.debug("Kill error output> {}", line);
							}
							read = new BufferedReader(new InputStreamReader(p2.getInputStream()));
							line = null;
							
							
							while((line = read.readLine()) != null)
							{
								log.debug("Kill output Input> {}", line);
							}
							
							
							int retValPGroup = p2.waitFor();
							
							if(retValPGroup > 0)
							{
								log.debug("SIGTERM to process group failed with error code {}", retValPGroup);
								Process p3 = Runtime.getRuntime().exec(SplitQuotedString.splitQuotedString(replacePid(options.procNiceKillCommand,pid)));
								int retVal = p3.waitFor();
								
								if(retVal > 0)
								{
									Object[] args = {  pid,retVal};
									log.debug("SIGTERM to process id: {} attempted failed with return code {}",args);
								} else
								{
									log.debug("SIGTERM delivered successfully to process id: {}", pid, pid);
								}
									
								
							} else
							{
								log.debug("SIGTERM delivered successfully to process group id: {} ", pid);
							}
						} catch (IOException e) {
							log.error("Couldn't SIGTERM process or process group ", e);
						}
						
					
						
						
						int totalSleepTime = 0;
						int currSleepTime = 25;
						while(true)
						{
							
							if(exited(p))
							{
								return;
							}
							
							Thread.sleep(currSleepTime);
							totalSleepTime += currSleepTime;
							currSleepTime *=1.5;
							if(totalSleepTime > 3000)
							{
								break;
							}
							
						}
												
						log.debug("Trying to send SIGKILL to process group id: {}", pid);
						try {
							Process p2 = Runtime.getRuntime().exec(SplitQuotedString.splitQuotedString(replacePid(options.pgForceKillCommand,pid)));
							int retVal = p2.waitFor();
							
							if(retVal > 0)
							{
								log.debug("SIGKILL to pid: {} attempted failed with return code {}",pid, retVal);
								
								Process p3 = Runtime.getRuntime().exec(SplitQuotedString.splitQuotedString(replacePid(options.procForceKillCommand,pid)));
								int retVal3 = p3.waitFor();
								
								if(retVal3 > 0)
								{
									Object[] args = {  pid,retVal};
									log.debug("SIGKILL to process id: {} attempted failed with return code {}",args);
								} else
								{
									log.debug("SIGKILL delivered successfully to process id: {}", pid, pid);
								}
								
								
							} else
							{
								log.debug("SIGKILL delivered successfully to pid: {} ", pid);
							}
						} catch (IOException e) {
							log.error("Couldn't SIGKILL process or process group ", e);
							
						}
					
						
						
					}
					
					p.destroy();
					p.waitFor();
				} finally{
					
					if(exited(p))
					{
						log.debug("Process pid: {} terminated successfully with return code {}", pid, p.exitValue());
					} else
					{
						log.warn("Process pid: {} was not terminated ", pid);
					}
					
				}	
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		} finally
		{
			this.processEnded.set(true);
			this.stopWallclockTimer();
			
		}
		
	}
	
	
	
	
	
}

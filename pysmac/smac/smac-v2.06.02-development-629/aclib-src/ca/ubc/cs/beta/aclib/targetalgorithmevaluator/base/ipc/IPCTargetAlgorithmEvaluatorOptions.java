package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.ipc;


import ca.ubc.cs.beta.aclib.misc.jcommander.validator.ValidPortValidator;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="Inter-Process Communication Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator hands the requests off to another process. The current encoding mechanism is the same as on the command line, except that we do not specify the algo executable field. The current mechanism can only execute one request to the server at a time. A small code change would be required to handle the more general case, so please contact the developers if this is required. ", level=OptionLevel.ADVANCED)
public class IPCTargetAlgorithmEvaluatorOptions extends AbstractOptions {

	
	
	@Parameter(names="--ipc-report-persistent", description="Whether the TAE should be treated as persistent, loosely a TAE is persistent if we could ask it for the same request later and it wouldn't have to redo the work from scratch.")
	public boolean persistent;
	
	@Parameter(names="--ipc-mechanism", description="Mechanism to use for IPC")
	public IPCMechanism ipcMechanism = IPCMechanism.UDP;
	
	@Parameter(names="--ipc-remote-host", description="Remote Host for some kinds of IPC mechanisms")
	public String remoteHost = "127.0.0.1";
	
	@Parameter(names="--ipc-remote-port", description="Remote Port for some kinds of IPC mechanisms", validateWith=ValidPortValidator.class)
	public int remotePort = 0;
	
	@Parameter(names="--ipc-udp-packetsize", description="Remote Port for some kinds of IPC mechanisms", validateWith=ValidPortValidator.class)
	public int udpPacketSize = 4096;

	enum IPCMechanism 
	{
		UDP
	}
	
	private static final long serialVersionUID = -7900348544680161087L;

}

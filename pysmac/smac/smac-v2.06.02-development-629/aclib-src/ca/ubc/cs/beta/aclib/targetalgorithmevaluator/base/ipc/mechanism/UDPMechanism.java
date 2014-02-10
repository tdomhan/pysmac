package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.ipc.mechanism;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;


import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aclib.misc.watch.StopWatch;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.ipc.ResponseParser;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

public class UDPMechanism {

	/**
	 * 
	 * @param rc
	 * @param execConfig
	 * @param port
	 * @param remoteAddr
	 * @param udpPacketSize
	 * @return
	 */
	public AlgorithmRun evaluateRun(RunConfig rc, AlgorithmExecutionConfig execConfig,  int port, String remoteAddr, int udpPacketSize) 
	{
		try {
			
			DatagramSocket clientSocket;
			clientSocket = new DatagramSocket();
		
			InetAddress IPAddress = InetAddress.getByName(remoteAddr);
			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			
			//outStream.println("Request:" + Arrays.deepToString(args) + " to: " + port);
			
			ArrayList<String> list = new ArrayList<String>();
			list.add(rc.getProblemInstanceSeedPair().getInstance().getInstanceName());
			list.add(rc.getProblemInstanceSeedPair().getInstance().getInstanceSpecificInformation());
			list.add(String.valueOf(rc.getCutoffTime()));
			list.add(String.valueOf(Integer.MAX_VALUE));
			list.add(String.valueOf(rc.getProblemInstanceSeedPair().getSeed()));
			
			StringFormat f = StringFormat.NODB_SYNTAX;
			
			for(String key : rc.getParamConfiguration().getActiveParameters()  )
			{
				
				
				if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
				{
					throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
				}
				list.add(f.getPreKey() + key);
				list.add(f.getValueDelimeter() + rc.getParamConfiguration().get(key)  + f.getValueDelimeter());	
				
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
	
			byte[] sendData;
			try {
				sendData = sb.toString().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
			
			if (sendData.length > udpPacketSize)
			{
				   throw new IllegalStateException("Response is too big to send to client, please adjust packetSize argument in both client and server " + sendData.length + " > " + udpPacketSize);	   
			}
			
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
			byte[] receiveData = new byte[udpPacketSize];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			StopWatch watch = new AutoStartStopWatch();
			clientSocket.send(sendPacket);
			
			clientSocket.receive(receivePacket);
			watch.stop();
			receiveData = receivePacket.getData();
			
			String response = new String(receiveData,"UTF-8");
		
			clientSocket.close();

			return ResponseParser.processLine(response, rc, execConfig, watch.time() / 1000);
			
		} catch (SocketException e1) {
			throw new TargetAlgorithmAbortException("TAE Aborted due to socket exception",e1);
		} catch(IOException e1)
		{
			throw new TargetAlgorithmAbortException("TAE Aborted due to IOException",e1);
		}
		
		
		
	}

}

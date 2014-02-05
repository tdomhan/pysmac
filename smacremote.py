import socket
import sys
import numpy as np
from itertools import izip

class SMACRemote(object):
    UDP_IP = "127.0.0.1"
    UDP_PORT = 5050
    #The size of a udp package
    #note: set in SMAC using --ipc-udp-packetsize
    UDP_PACKAGE_SIZE = 4096
    SOCKET_TIMEOUT = 3

    def __init__(self):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self._sock.settimeout(SMACRemote.SOCKET_TIMEOUT)

        #try to find a free port:
        for self.port in range(SMACRemote.UDP_PORT, SMACRemote.UDP_PORT+1000):
            try:
                self._sock.bind((SMACRemote.UDP_IP, self.port))
                break
            except:
                pass
        print "Found port: ", self.port

    def __del__(self):
        self._sock.close()
 
    def send(self, data):
        #        print ">",
        #line = sys.stdin.readline()
        self._sock.sendto(data, self._smac_addr)

    def receive(self):
        print "Waiting for a message from SMAC."

        data, addr = self._sock.recvfrom(4096) # buffer size is 1024 bytes
        self._smac_addr = addr
        print "<", data
        return data

    def get_next_parameters(self):
        """
            Fetch the next set of parameters.

            returns: an array of parameters.
        """
        data = self.receive()
        return self._convert_smac_param_string(data)

    def _convert_smac_param_string(self, string):
        """
            SMAC format:
                instance0 0 18000.0 2147483647 4 -x0 '6.9846789681200185' -x1 '13.43140264469383'
        """
        #TODO: maybe use the seed paramter

        #split and remove the leading inforation
        params = string.strip().split()[5:]
        param_pairs = [pair for pair in izip(*[iter(params)]*2)]
        x = np.zeros(len(param_pairs))
        for param_name, param_value in param_pairs:
            #strip "-x" from the name
            param_index = int(param_name.strip("-x"))
            param_value = float(param_value.strip("'"))
            print param_index, " ", param_value
            x[param_index] = param_value

        return x
        
    
    def report_performance(self, performance, runtime):
        """
            Report the performance for the current run of the algorithm.

            performance: performance of the algorithm.
            runtime: the runtime of the call in seconds.
        """
        #format:
        #Result for ParamILS: <solved>, <runtime>, <runlength>, <quality>, <seed>, <additional rundata>
        #e.g. Result for ParamILS: UNSAT, 6,0,0,4
        
        #runtime must be strictly positive:
        runtime = min(0, runtime)
        data = "Result for ParamILS: SAT, %f, 0, %f, 4" % (runtime, performance)
        print "Response to SMAC:"
        print data
        self.send(data)

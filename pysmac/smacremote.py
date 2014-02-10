import socket
import sys

from pysmac.smacparse import parse_smac_param_string

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
        print "Communicating on port: ", self.port

    def __del__(self):
        self._sock.close()
 
    def send(self, data):
        self._sock.sendto(data, self._smac_addr)

    def receive(self):
        #print "Waiting for a message from SMAC."

        data, addr = self._sock.recvfrom(4096) # buffer size is 1024 bytes
        self._smac_addr = addr
        #print "<", data
        return data

    def get_next_parameters(self):
        """
            Fetch the next set of parameters.

            returns: an array of parameters.
        """
        param_string = self.receive()
        return parse_smac_param_string(param_string)
    
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
        #print "Response to SMAC:"
        #print data
        self.send(data)

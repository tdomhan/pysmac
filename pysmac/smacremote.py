import socket
import sys
import logging

from pysmac.smacparse import parse_smac_param_string

class SMACRemote(object):
    IP = "127.0.0.1"
    TCP_PORT = 5050
    #The size of a udp package
    #note: set in SMAC using --ipc-udp-packetsize

    def __init__(self):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self._conn = None

        #try to find a free port:
        for self.port in range(SMACRemote.TCP_PORT, SMACRemote.TCP_PORT+1000):
            try:
                self._sock.bind((SMACRemote.IP, self.port))
                self._sock.listen(1)
                break
            except:
                pass
        logging.debug("Communicating on port: %d", self.port)

    def __del__(self):
        if self._conn is not None:
            self._conn.close()
        self._sock.close()

    def send(self, data):
        assert self._conn is not None

        logging.debug("> " + str(data))
        self._conn.sendall(data)

        self._conn.close()
        self._conn = None

    def receive(self):
        logging.debug("Waiting for a message from SMAC.")
        self._conn, addr = self._sock.accept()

        #data = self._conn.recv(4096) # buffer size is 4096 bytes
        fconn = self._conn.makefile('r') 
        data = fconn.readline()

        logging.debug("< " + str(data))
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
        logging.debug("Response to SMAC:")
        logging.debug(str(data))

        self.send(data)

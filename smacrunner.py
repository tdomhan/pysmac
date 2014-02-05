import tempfile
import shutil
import os
from subprocess import Popen

import numpy as np

class SMACRunner(object):
    """
        Interface to the SMAC library:
        see: http://www.cs.ubc.ca/labs/beta/Projects/SMAC/
    """

    def __init__(self, x0, xmin, xmax, port, max_evaluations):
        """
            Start up SMAC.

            x0: initial guess
            xmin: minimum values
            xmax: maximum values 
            port: the port to communicate with SMACRemote
            max_evaluations: the maximum number of evaluations
        """
        self.x0 = x0
        self.xmin = xmin
        self.xmax = xmax
        self._port = port
        self._max_evaluations = max_evaluations

        self._create_working_dir()
        self._generate_scenario_file()
        self._generate_instance_file()
        self._generate_parameter_file()
        self._start_smac()

    def __del__(self):
        print "Shutting down SMACRunner."
        if hasattr(self, "_working_dir"):
            shutil.rmtree(self._working_dir)
        if hasattr(self, "_smac_process"):
            self._smac_process.poll()
            if self._smac_process.returncode == None:
                self._smac_process.kill()

    def is_finished(self):
        """
            Has the SMAC run finished?
        """
        self._smac_process.poll()
        if self._smac_process.returncode == None:
            return False
        else:
            return True

    def get_best_parameters(self):
        """
            Get the best parameters that were found.
        """
        return np.asarray([0, 0])

    def _create_working_dir(self):
        self._working_dir = tempfile.mkdtemp()

    def _generate_scenario_file(self):
        """
            Generate a scenario file in order to run SMAC.
        """
        parameters = {'working_dir': self._working_dir}
        fdata = """
algo = echo 0
execdir = %(working_dir)s
deterministic = 1
run_obj = quality
overall_obj = mean
cutoff_time = 18000
cutoff_length = max
validation = false
paramfile = %(working_dir)s/params.pcs
instance_file = %(working_dir)s/instances.txt
""" % parameters
#tunerTimeout = 36000
#test_instance_file = %(working_dir)s/instances-test.txt
        self._scenario_file_name = os.path.join(self._working_dir, "smac-scenario.txt")
        with open(self._scenario_file_name, "w")  as self.scenario_file:
            self.scenario_file.write(fdata)

    def _generate_instance_file(self):
        instance_file_name = os.path.join(self._working_dir, "instances.txt")
        with open(instance_file_name, "w") as instance_file:
            instance_file.write("instance0")
            
    def _generate_parameter_file(self):
        param_definitions = []
        for i, (min_val, max_val, default_val) in enumerate(zip(self.xmin, self.xmax, self.x0)):
            param_definitions.append("x%d [%f, %f] [%f]" % (i, min_val, max_val, default_val))
        param_file_name = os.path.join(self._working_dir, "params.pcs")
        with open(param_file_name, "w")  as param_file:
            param_file.write("\n".join(param_definitions))
        print param_file_name


    def _start_smac(self):
        """
            Start SMAC in IPC mode. SMAC will wait for udp messages to be sent.
        """
        cmds = ["/Users/tdomhan/Projects/pysmac/smac/smac-v2.06.02-development-629/smac",
                "--scenario-file", self._scenario_file_name,
                "--num-run","1",
                "--totalNumRunsLimit", str(self._max_evaluations),
                "--tae","IPC",
                "--ipc-remote-port", str(self._port)]
        self._smac_process = Popen(cmds)



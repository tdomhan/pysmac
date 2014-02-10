import tempfile
import shutil
import os
import sys
import time
from subprocess import Popen
from pkg_resources import resource_filename

from pysmac.smacparse import parse_smac_trajectory_string

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
            Get the best parameters that were found along with the according function value.

            returns: (x, f(x))
        """
        with open(os.path.join(self._result_dir, "traj-run-1.txt"), "r") as traj_file:
            best_config_str = traj_file.readlines()[-1].strip()
            return parse_smac_trajectory_string(best_config_str)
        return (np.asarray([]), 0)

    def _create_working_dir(self):
        self._working_dir = tempfile.mkdtemp()
        self._exec_dir = os.path.join(self._working_dir, "exec")
        self._out_dir = os.path.join(self._working_dir, "out")
        os.mkdir(self._exec_dir)
        os.mkdir(self._out_dir)
        #also see the rungroup parameter
        self._result_dir = os.path.join(self._out_dir, "result")

    def _generate_scenario_file(self):
        """
            Generate a scenario file in order to run SMAC.
        """
        parameters = {'working_dir': self._working_dir,
                      'exec_dir': self._exec_dir,
                      'out_dir': self._out_dir}
        fdata = """
algo = echo 0
execdir = %(exec_dir)s
outdir = %(out_dir)s
deterministic = 1
rungroup = result
run_obj = quality
overall_obj = mean
cutoff_time = 18000
cutoff_length = max
validation = false
paramfile = %(working_dir)s/params.pcs
instance_file = %(working_dir)s/instances.txt
""" % parameters
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


    def _start_smac(self):
        """
            Start SMAC in IPC mode. SMAC will wait for udp messages to be sent.
        """
        smac_executable = "smac"
        if sys.platform in ["win32", "cygwin"]:
            #we're on windows
            smac_executable = "smac.bat"
        smac_path = resource_filename(__name__, 'smac/smac-v2.06.02-development-629/%s' % smac_executable)
        print "SMAC path: ", smac_path
        cmds = [smac_path,
                "--scenario-file", self._scenario_file_name,
                "--num-run","1",
                "--totalNumRunsLimit", str(self._max_evaluations),
                "--tae","IPC",
                "--ipc-remote-port", str(self._port)]
        with open(os.devnull, "w") as fnull:
            self._smac_process = Popen(cmds, stdout = fnull, stderr = fnull)


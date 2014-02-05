import numpy as np
from itertools import izip

def param_pairs_to_np(param_pairs):
    x = np.zeros(len(param_pairs))
    for param_name, param_value in param_pairs:
        #strip "-x" from the name
        param_index = int(param_name.strip("-x"))
        param_value = float(param_value.strip("'"))
        x[param_index] = param_value
    return x
 

def parse_smac_param_string(param_string):
    """
        SMAC format:
            instance0 0 18000.0 2147483647 4 -x0 '6.9846789681200185' -x1 '13.43140264469383'
    """
    params = param_string.strip().split()[5:]
    param_pairs = [pair for pair in izip(*[iter(params)]*2)]
    return param_pairs_to_np(param_pairs)

def parse_smac_trajectory_string(param_string):
    """
        SMAC format:
          12.95960999999998, 0.542677, 2.103, 100, 2.9599830000000003,  x0='3.2343299868854594', x1='1.8820288649037564'

        returns: (x, fval)
    """
    columns = param_string.strip().split(",")

    fval = float(columns[1])

    params = columns[5:]
    param_pairs = [param.strip().split("=") for param in params]

    return (param_pairs_to_np(param_pairs), fval)


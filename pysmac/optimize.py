import numpy as np

import time

from socket import timeout

from pysmac.smacrunner import SMACRunner
from pysmac.smacremote import SMACRemote


def fmin(objective, x0, xmin, xmax, max_evaluations=100, **args):
    """
        min_x f(x) s.t. xmin < x < xmax

        objective: The objective function that should be optimized.
                   Designed for objective functions that are:
                   costly to calculate + don't have a derivative available.
        x0: initial guess
        xmin: minimum values 
        xmax: maximum values
        max_evaluations: the maximum number of evaluations to execute
        args: extra parameters to pass to the objective function

        returns: best parameters found
    """
    x0 = np.asarray(x0)
    xmin = np.asarray(xmin)
    xmax = np.asarray(xmax)
    assert(x0.shape == xmin.shape), "shape of x0 and xmin don't agree"
    assert(x0.shape == xmax.shape), "shape of x0 and xmax don't agree"

    smacremote = SMACRemote()

    smacrunner = SMACRunner(x0, xmin, xmax, smacremote.port, max_evaluations)

    while not smacrunner.is_finished():
        try:
            params = smacremote.get_next_parameters()
        except timeout:
            #Timeout, check if the runner is finished
            continue

        start = time.clock()
        performance = objective(params, **args)
        runtime = time.clock() - start

        smacremote.report_performance(performance, runtime)

    return smacrunner.get_best_parameters()



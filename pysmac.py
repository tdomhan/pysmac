import numpy as np

"""
RANDOM NOTES:

Function design:
    fmin(objective, x0, xmin, xmax, params)
        min_x f(x) s.t. xmin < x < xmax
    objective: The objective function that should be optimized.
               Designed for objective functions that are:
               costly to calculate + don't have a  derivative available. 

    Minimal example:
        import numpy as np

         #Branin function
         def branin(x):
            x1 = x[0]
            x2 = x[1]
            a = 1.
            b = 5.1 / (4.*np.pi**2)
            c = 5. / np.pi
            r = 6.
            s = 10.
            t = 1. / (8.*np.pi)
            ret  = a*(x2-b*x1**2+c*x1-r)**2+s*(1-t)*np.cos(x1)+s
            print ret
            return ret


         from pysmac import fmin
         fmin(branin, (0, 0), (-5, 0), (10, 15))

     Compare scipy:
         from scipy.optimize import fmin

         fmin(branin, x0=(0,0))
         Optimization terminated successfully.
                  Current function value: 0.397887
                  Iterations: 80
                  Function evaluations: 152
         Out[33]: array([ 3.1416057,  2.2749845])

     Compare hyperopt:
        from hyperopt import fmin, tpe, hp

        space = [hp.uniform(x0, -5, 10),hp.uniform(x1, 0, 15)]
        best = fmin(branin, space=space, algo=tpe.suggest, max_evals=300)
        
"""


def fmin(objective, x0, xmin, xmax, **params):
    """
        min_x f(x) s.t. xmin < x < xmax

        objective: The objective function that should be optimized.
                   Designed for objective functions that are:
                   costly to calculate + don't have a derivative available.
        xmin: minimum values 
        xmax: maximum values
        params: extra parameters to pass to the objective function
    """
    x0 = np.asarray(x0)
    xmin = np.asarray(xmin)
    xmax = np.asarray(xmax)
    assert(x0.shape == xmin.shape), "shape of x0 and xmin don't agree"
    assert(x0.shape == xmax.shape), "shape of x0 and xmax don't agree"




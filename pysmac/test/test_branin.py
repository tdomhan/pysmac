import unittest

from pysmac.optimize import fmin
import numpy as np


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


class OptimizerTest(unittest.TestCase):

    def test_branin(self):
        """
            Check if we can find the branin minimum.
        """
        xmin, fval = fmin(branin, x0=(0,0),xmin=(-5, 0), xmax=(10, 15), max_evaluations=5000)
        self.assertEqual(len(xmin), 1)
        self.assertEqual(len(xmin["x"]), 2)
        self.assertAlmostEqual(fval, 0.397887, places=2) 


def main():
    unittest.main()

if __name__ == '__main__':
    main()

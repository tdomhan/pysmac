import unittest

import numpy as np

from pysmac.smacparse import parse_smac_param_string
from pysmac.smacparse import parse_smac_trajectory_string

class SMACParserTest(unittest.TestCase):

    def test_param_string(self):
        param_string = "instance0 0 18000.0 2147483647 4 -x0 '6.9846789681200185' -x1 '13.43140264469383'" 
        x_expected = np.asarray([6.9846789681200185, 13.43140264469383])
        x = parse_smac_param_string(param_string)

        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))


    def test_trajectory_string(self):
        param_string = "12.847553999999981, 0.542677, 1.979, 100, 2.847841,  x0='3.2343299868854594', x1='1.8820288649037564'"
        x_expected = np.asarray([3.2343299868854594, 1.8820288649037564])
        fval_expected = 0.542677

        x, fval = parse_smac_trajectory_string(param_string)

        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))
        self.failUnless(np.allclose([fval], [fval_expected], rtol=1e-05, atol=1e-08))

def main():
    unittest.main()

if __name__ == '__main__':
    main()

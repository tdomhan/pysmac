import unittest

import numpy as np

from pysmac.smacparse import parse_smac_param_string
from pysmac.smacparse import parse_smac_trajectory_string

class SMACParserTest(unittest.TestCase):

    def test_param_string_simple(self):
        param_string = "instance0 0 18000.0 2147483647 4 -x0 '6.9846789681200185' -x1 '13.43140264469383'" 
        x_expected = np.asarray([6.9846789681200185, 13.43140264469383])
        params = parse_smac_param_string(param_string)
        x = params["x"]

        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))

    def test_param_string_int(self):
        param_string = "instance0 0 18000.0 2147483647 4 -x_int0 '6' -x_int1 '13'" 
        x_expected = np.asarray([6, 13])
        params = parse_smac_param_string(param_string)
        x = params["x_int"]
        self.assertTrue(x.dtype == np.int)

        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))

    def test_param_string_categorical(self):
        param_string = "instance0 0 18000.0 2147483647 4 -x_categorical_param0 '6' -x_categorical_param1 'test'" 
        params = parse_smac_param_string(param_string)
        x = params["x_categorical"]
        
        self.assertEqual(x, {"param0": 6., "param1": "test"})

    def test_param_string_mixed(self):
        param_string = "instance0 0 18000.0 2147483647 4  -x0 '6.9846789681200185' -x_int0 '6' -x_categorical_param1 'test' -x1 '13.43140264469383' -x_int1 '13'" 
        params = parse_smac_param_string(param_string)

        x = params["x"]
        x_expected = np.asarray([6.9846789681200185, 13.43140264469383])
        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))
 
        x_int = params["x_int"]
        self.failUnless(np.allclose(x_int, np.asarray([6, 13]), rtol=1e-05, atol=1e-08))
        self.assertTrue(x_int.dtype == np.int)

        x = params["x_categorical"]
        self.assertEqual(x, {"param1": "test"})

    def test_trajectory_string(self):
        param_string = "12.847553999999981, 0.542677, 1.979, 100, 2.847841,  x0='3.2343299868854594', x_int0='6', x_int1='13', x1='1.8820288649037564', x_categorical_param1='test'"
        x_expected = np.asarray([3.2343299868854594, 1.8820288649037564])
        fval_expected = 0.542677

        params, fval = parse_smac_trajectory_string(param_string)
        self.failUnless(np.allclose([fval], [fval_expected], rtol=1e-05, atol=1e-08))

        x = params["x"]
        self.failUnless(np.allclose(x, x_expected, rtol=1e-05, atol=1e-08))

        x_int = params["x_int"]
        x_int_expected = np.asarray([6, 13])
        self.failUnless(np.allclose(x_int, x_int_expected, rtol=1e-05, atol=1e-08))
        self.assertTrue(x_int.dtype == np.int)

        x_cat = params["x_categorical"]
        self.assertEqual(x_cat, {"param1": "test"})


def main():
    unittest.main()

if __name__ == '__main__':
    main()

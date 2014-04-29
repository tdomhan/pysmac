import unittest
import copy

from pysmac.optimize import fmin
import numpy as np


class OptimizerTest(unittest.TestCase):

    def test_int_params(self):
        """
            We check that using int parameters work
            but comparing the expected and the actual
            default value.
        """
        def test_fun(x_int):
            self.assertEqual(x_int[0], 4)
            self.assertEqual(x_int[1], 19)
            return 1
        xmin, fval = fmin(test_fun,
                          x0_int=(4,19),
                          xmin_int=(-100, -100),
                          xmax_int=(100, 100),
                          max_evaluations=1)

    def test_float_params(self):
        """
            Check if we can find the branin minimum.
        """
        def test_fun(x):
            self.assertAlmostEqual(x[0], 3.)
            self.assertAlmostEqual(x[1], 20.)
            return 1
        xmin, fval = fmin(test_fun,
                          x0=(3.,20.),
                          xmin=(-100, -100),
                          xmax=(100, 100),
                          max_evaluations=1)

    def test_categorical_params(self):
        """
            Check if we can find the branin minimum.
        """
        def test_fun(x_categorical):
            self.assertEqual(x_categorical,
                {"test1": "string",
                 "test2": 2})
            return 1
        xmin, fval = fmin(test_fun,
            x_categorical={"test1": ["string"],
                "test2": [2]},
            max_evaluations=1)


    def test_objective_no_return_value(self):
        """
            We want to make sure the objective function always returns something.
        """
        def test_fun(x):
            return None
        self.assertRaises(AssertionError, 
                          fmin,
                          test_fun,
                          x0=(3.,20.),
                          xmin=(-100, -100),
                          xmax=(100, 100),
                          max_evaluations=1)

    def test_custom_function_args(self):
        def test_fun(x_int, custom_arg1, custom_arg2):
            self.assertEqual(custom_arg1, 1)
            self.assertEqual(custom_arg2, "some_string")
            return 1
        xmin, fval = fmin(test_fun,
                          x0_int=(4,19),
                          xmin_int=(-100, -100),
                          xmax_int=(100, 100),
                          max_evaluations=1,
                          custom_args={"custom_arg1": 1,
                                       "custom_arg2": "some_string"})


def main():
    unittest.main()

if __name__ == '__main__':
    main()

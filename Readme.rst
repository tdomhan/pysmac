pysmac
======

Simple python wrapper to `SMAC`_

::

     fmin(objective, x0, xmin, xmax, x0_int, xmin_int, xmax_int, xcategorical, params)
        min_x f(x) s.t. xmin < x < xmax
        
      objective: The objective function that should be optimized.

Installation
------------

Pip
~~~

::

    pip install pysmac


Example usage
-------------

Let’s take for example the Branin function:

.. code:: python

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

For x1 ∈ [-5, 10], x2 ∈ [0, 15] the function reaches a minimum value of:
*0.397887*.

Note: fmin accepts any function that has a parameter called ``x`` (the
input array) and returns an objective value.

.. code:: python

    from pysmac.optimize import fmin

    xmin, fval = fmin(branin, x0=(0,0),xmin=(-5, 0), xmax=(10, 15), max_evaluations=5000)

As soon as the evaluations are finished, we can check the output:

.. code:: python

    >>> xmin
    {'x': array([ 3.14305644,  2.27827543])}

    >>> fval
    0.397917

Let’s run the objective function with the found parameters:

.. code:: python

    >>> branin(**xmin)
    0.397917

Advanced
--------

Custom arguments to the objective function:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note: make sure there is no naming collission with the parameter names
and the custom arguments.

.. code:: python

    def minfunc(x, custom_arg1, custom_arg2):
        print "custom_arg1:", custom_arg1
        print "custom_arg2:", custom_arg2
        return 1


    xmin, fval = fmin(minfunc, x0=(0,0),xmin=(-5, 0), xmax=(10, 15),
                      max_evaluations=5000,
                      custom_arg1="test",
                      custom_arg2=123)

Integer parameters
~~~~~~~~~~~~~~~~~~

Integer parameters can be encoded as follows:

.. code:: python


    def minfunc(x, x_int):
        print "x: ", x
        print "x_int: ", x_int
        return 1.

    xmin, fval = fmin(minfunc,
                      x0=(0,0), xmin=(-5, 0), xmax=(10, 15),
                      x0_int=(0,0), xmin_int=(-5, 0), xmax_int=(10, 15),
                      max_evaluations=5000)

Categorical parameters
~~~~~~~~~~~~~~~~~~~~~~

Categorical parameters can be specified as a dictionary of lists of
values they can take on, e.g.:

.. code:: python

    categorical_params = {"param1": [1,2,3,4,5,6,7],
                          "param2": ["string1", "string2", "string3"]}

    def minfunc(x_categorical):
        print "param1: ", x_categorical["param1"]
        print "param2: ", x_categorical["param2"]
        return 1.

    xmin, fval = fmin(minfunc,
                      x_categorical=categorical_params,
                      max_evaluations=5000)

.. _SMAC: http://www.cs.ubc.ca/labs/beta/Projects/SMAC/

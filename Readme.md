pysmac
======

Simple python wrapper to [SMAC](http://www.cs.ubc.ca/labs/beta/Projects/SMAC/)

```
 fmin(objective, x0, xmin, xmax, params)
    min_x f(x) s.t. xmin < x < xmax
    
  objective: The objective function that should be optimized.
           Designed for objective functions that are:
           costly to calculate + don't have a  derivative available.
```

Installation
------------

```
python setup.py install
```
 
Usage
-----

Let's take for example the Branin function(Note: any function that takes in an array and returns a value can be minimized):
```python
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
```
For x1 ∈ [-5, 10], x2 ∈ [0, 15] the function reaches a minimum value of: *0.397887*.

```python
from pysmac.optimize import fmin

xmin, fval = fmin(branin, x0=(0,0),xmin=(-5, 0), xmax=(10, 15), max_evaluations=5000)
```
As soon as the evaluations are finished, we can check the output:
```python
>>> xmin
array([ 3.14305644,  2.27827543])

>>> fval
0.397917
```


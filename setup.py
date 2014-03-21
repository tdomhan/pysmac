from setuptools import setup, find_packages

"""
    for the long description, convert:
        https://coderwall.com/p/qawuyq
    or manually here:
        http://johnmacfarlane.net/pandoc/try/
"""

def check_java_exists():
    from subprocess import call
    import os
    try:
        devnull = open(os.devnull, 'w')
        call("java", stdout=devnull, stderr=devnull)
    except:
        error_msg = """
        Java not found!

        pysmac needs java in order to work. You can download java from:
        http://java.com/getjava
        """
        raise RuntimeError(error_msg)

check_java_exists()

setup(
    name = "pysmac",
    version = "0.4",
    packages = find_packages(),
    install_requires = ['numpy', 'docutils>=0.3', 'setuptools'],
    author = "Tobias Domhan (python wrapper). Frank Hutter, Holger Hoos, Kevin Leyton-Brown, Kevin Murphy and Steve Ramage (SMAC)",
    author_email = "domhant@informatik.uni-freiburg.de",
    description = "python interface to the hyperparameter optimization tool SMAC.",
    include_package_data = True,
    test_suite = 'pysmac.test.test_smacparse',
    keywords = "hyperparameter parameter optimization hyperopt bayesian smac global",
    license = "SMAC is free for academic & non-commercial usage. Please contact Frank Hutter(fh@informatik.uni-freiburg.de) to discuss obtaining a license for commercial purposes.",
    url = "http://www.cs.ubc.ca/labs/beta/Projects/SMAC/"
)

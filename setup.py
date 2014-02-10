from setuptools import setup, find_packages
setup(
    name = "pysmac",
    version = "0.1",
    packages = find_packages(),
    install_requires = ['numpy', 'docutils>=0.3', 'setuptools'],
    author = "Tobias Domhan",
    author_email = "domhant@informatik.uni-freiburg.de",
    description = "python interface to SMAC.",
    include_package_data = True,
    test_suite = 'pysmac.test.test_smacparse',
    keywords = "hyperparameter optimization hyperopt bayesian smac",
    url = "http://www.cs.ubc.ca/labs/beta/Projects/SMAC/"
)

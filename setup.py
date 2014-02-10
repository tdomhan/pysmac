from setuptools import setup, find_packages

def check_java_exists():
    from subprocess import call
    try:
        call("java")
    except:
        error_msg = """
        Java not found!

        pysmac needs java in order to work. You can download java from:
        java.com/getjava
        """
        raise RuntimeError(error_msg)

check_java_exists()

setup(
    name = "pysmac",
    version = "0.1",
    packages = find_packages(),
    install_requires = ['numpy', 'docutils>=0.3', 'setuptools'],
    author = "Tobias Domhan",
    author_email = "domhant@informatik.uni-freiburg.de",
    description = "python interface to SMAC.",
    # we need a long restructured text description
    #    long_description = open('Readme.md').read(),
    include_package_data = True,
    test_suite = 'pysmac.test.test_smacparse',
    keywords = "hyperparameter optimization hyperopt bayesian smac",
    url = "http://www.cs.ubc.ca/labs/beta/Projects/SMAC/"
)

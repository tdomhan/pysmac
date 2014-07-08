#example taken from:
#http://scikit-learn.org/stable/auto_examples/randomized_search.html
import numpy as np

from time import time
from operator import itemgetter
from scipy.stats import randint as sp_randint

from sklearn.grid_search import GridSearchCV, RandomizedSearchCV
from sklearn.datasets import load_digits
from sklearn.ensemble import RandomForestClassifier
from sklearn.cross_validation import cross_val_score

# get some data
iris = load_digits()
X, y = iris.data, iris.target

# build a classifier
clf = RandomForestClassifier(n_estimators=20)


# Utility function to report best scores
def report(grid_scores, n_top=3):
    top_scores = sorted(grid_scores, key=itemgetter(1), reverse=True)[:n_top]
    for i, score in enumerate(top_scores):
        print("Model with rank: {0}".format(i + 1))
        print("Mean validation score: {0:.3f} (std: {1:.3f})".format(
              score.mean_validation_score,
              np.std(score.cv_validation_scores)))
        print("Parameters: {0}".format(score.parameters))
        print("")


# specify parameters and distributions to sample from
param_dist = {"max_depth": [3, None],
              "max_features": sp_randint(1, 11),
              "min_samples_split": sp_randint(1, 11),
              "min_samples_leaf": sp_randint(1, 11),
              "bootstrap": [True, False],
              "criterion": ["gini", "entropy"]}

# run randomized search
n_iter_search = 20
random_search = RandomizedSearchCV(clf, param_distributions=param_dist,
                                   n_iter=n_iter_search)

start = time()
random_search.fit(X, y)
print("RandomizedSearchCV took %.2f seconds for %d candidates"
      " parameter settings." % ((time() - start), n_iter_search))
report(random_search.grid_scores_)

# use a full grid over all parameters
param_grid = {"max_depth": [3, None],
              "max_features": [1, 3, 10],
              "min_samples_split": [1, 3, 10],
              "min_samples_leaf": [1, 3, 10],
              "bootstrap": [True, False],
              "criterion": ["gini", "entropy"]}

# run grid search
grid_search = GridSearchCV(clf, param_grid=param_grid)
start = time()
grid_search.fit(X, y)

print("GridSearchCV took %.2f seconds for %d candidate parameter settings."
      % (time() - start, len(grid_search.grid_scores_)))
report(grid_search.grid_scores_)

#pySMAC
from pysmac.optimize import fmin

def objective(x_int, x_categorical, clf, X, y):
  max_features = x_int[0]
  min_samples_split = x_int[1]
  min_samples_leaf = x_int[2]

  clf.set_params(max_depth=x_categorical["max_depth"],
                 bootstrap=x_categorical["bootstrap"],
                 criterion=x_categorical["criterion"],
                 max_features=max_features,
                 min_samples_split=min_samples_split,
                 min_samples_leaf=min_samples_leaf)
  #we want to minimize and cross_val_score is the accuracy:
  scores = cross_val_score(clf, X, y)
  return -1 * np.mean(scores)


categorical_params = {"max_depth": [3, None],
                      "bootstrap": [True, False],
                      "criterion": ["gini", "entropy"]}

start = time()
xmin, fval = fmin(objective,
  x0_int=(3,3,3), xmin_int=(1,1,1), xmax_int=(10, 10, 10),
  x_categorical=categorical_params,
  max_evaluations=n_iter_search,
  custom_args={"X": X, "y": y, "clf": clf})
print("SMAC took %.2f seconds for %d candidate parameter settings."
      % (time() - start, n_iter_search))
print "Best configuration found: ", xmin
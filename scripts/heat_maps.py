import sys
import json
import numpy as np
import matplotlib.pyplot as plt
from math import log

if len(sys.argv) < 2:
    raise AttributeError("The name of the file is not specified")

with open(sys.argv[1], 'r') as myfile:
    data = myfile.read()
obj = json.loads(data)

# Saves the heat map of the pivot vs the probability, after fixing the value of the variable
def hm_pivot_proba(model_name, var_value):
    times = obj[model_name]['tableHash']
    typed_values = {int(pivot):{float(proba):log(int(time)/1000000) for proba, time in pivot_data[str(var_value)].items()}for pivot,pivot_data in times.items()}
    pivot_values = sorted(typed_values.keys())
    proba_values = sorted(typed_values[pivot_values[0]].keys())
    times = [[typed_values[pivot][proba] for pivot in pivot_values] for proba in proba_values]
    plot("pivot", pivot_values, "probabilité", ["1/"+str(int(1/p+0.001)) for p in proba_values], times, model_name, "Blues")

# Saves the heat map of the variables vs the probability, after fixing the value of the pivot
def hm_var_proba(model_name, pivot_value):
    times = obj[model_name]['tableHash']
    typed_values = {int(var):{float(proba):log(int(time)/1000000) for proba, time in var_data.items()} for var, var_data in times[str(pivot_value)].items()}
    var_values = sorted(typed_values.keys())
    proba_values = sorted(typed_values[var_values[0]].keys())
    times = [[typed_values[var][proba] for var in var_values] for proba in proba_values]
    plot("nb_vars", var_values, "probabilité", ["1/"+str(int(1/p+0.001)) for p in proba_values], times, model_name, "Greens")

# Saves the heat map of the variables vs the pivot, after fixing the value of the probability
def hm_var_pivot(model_name, proba_value):
    times = obj[model_name]['tableHash']
    typed_values = {int(pivot):{int(var):log(int(time[str(proba_value)])/1000000) for var, time in pivot_data.items()} for pivot, pivot_data in times.items()}
    pivot_values = sorted(typed_values.keys(), reverse = True)
    var_values = sorted(typed_values[pivot_values[0]].keys())
    times = [[typed_values[pivot][var] for var in var_values] for pivot in pivot_values]
    plot("nb_vars", var_values, "pivot", pivot_values, times, model_name, "Reds")

# plots a heat map
def plot(Xname, Xvals, Yname, Yvals, times, model_name, color):
    plt.xticks(ticks=np.arange(len(Xvals)),labels=Xvals)
    plt.xlabel(Xname)
    plt.yticks(ticks=np.arange(len(Yvals)),labels=Yvals)
    plt.ylabel(Yname)
    hm=plt.imshow(times, cmap=color,interpolation="nearest")
    plt.colorbar(hm, label="Temps (en ms)")

# save multiple heat maps
def saved_graphs(model_name, pivot_value, var_value, proba_value):
    hm_pivot_proba(model_name, var_value)
    plt.title("nb_vars = "+ str(var_value))
    plt.savefig('heat_map_var.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()
    hm_var_pivot(model_name, proba_value)
    plt.title("probabilité = 1/"+str(int(1/proba_value+0.001)))
    plt.savefig('heat_map_proba.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()
    hm_var_proba(model_name, pivot_value)
    plt.title("pivot = "+ str(pivot_value))
    plt.savefig('heat_map_pivot.svg', bbox_inches='tight',pad_inches = 0)
    
if __name__ == "__main__":
    print("Models present in the file :", list(obj.keys()))
    #model_name = "12-queens"
    #model_name = "Megane"
    model_name = "oc-roster-4s-10d.czn-3"
    saved_graphs(model_name, 16, 3, 1/16) # One should change the parameters here





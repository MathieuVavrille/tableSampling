import json
import sys
import numpy as np
import matplotlib.pyplot as plt
from math import log

if len(sys.argv) < 2:
    raise AttributeError("The name of the file is not specified")

with open(sys.argv[1], 'r') as myfile:
    data = myfile.read()
obj = json.loads(data)

obj_typed = {model_name:
             {'random':models['random'],
              'table': {
                  int(pivot_str):{
                      int(vars_str):{
                          int(1/float(proba)+0.0001):[float(d) for d in data_list]
                          for proba,data_list in vars_data.items()}
                      for vars_str, vars_data in pivots.items()}
                  for pivot_str, pivots in models['tableHash'].items()}}
             for model_name, models in obj.items()}

# Plots a graph where there are different pivots
def variate_pivot(model, var, proba):
    random = obj_typed[model]['random']
    plt.plot(random, label="RandomVarDom")
    all_data = obj_typed[model]['table']
    data = [(2,all_data[2][var][proba]), (4,all_data[4][var][proba]), (8,all_data[8][var][proba]), (16,all_data[16][var][proba])]
    for pivot, l in data:
        t = (log(pivot,2)-1)/3
        plt.plot(l, label=f'table sampling κ={pivot}', color=((1-t)*1.0+t*186/255, (1-t)*137/255, (1-t)*137/255))
    plt.xlabel('Number of samples')
    plt.ylabel('p-value')
    plt.yscale('log')
    plt.legend()
    plt.savefig('pivot.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()

def find_first_below(l, bound):
    lb = [i for i in l if i < bound]
    if lb:
        return l.index(lb[0])
    else:
        return -1

# Plots a graph where there are different number of variables
def variate_var(model, pivot, proba):
    random = obj_typed[model]['random']
    print(find_first_below(random, 10**-7))
    plt.plot(random, label="RandomVarDom")
    pivots = obj_typed[model]['table'][pivot]
    data = [(2,pivots[2][proba]), (3,pivots[3][proba]), (4,pivots[4][proba]), (5,pivots[5][proba])]
    for v, l in data:
        t = (v-2)/3
        plt.plot(l[:l.index(0)+2] if 0 in l else l, label=f'table sampling v={v}', color=((1-t)*1.0+t*186/255, (1-t)*137/255, (1-t)*137/255))
        print(v, find_first_below(l, 10**-7))
    plt.axhline(10**-7, color = "black", linestyle = "dotted")
    plt.xlabel('Number of samples')
    plt.ylabel('p-value')
    plt.yscale('log')
    plt.legend()
    plt.savefig('var.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()

# Plots a graph where there are different probabilities
def variate_proba(model, pivot, var):
    random = obj_typed[model]['random']
    plt.plot(random, label="RandomVarDom")
    probas = obj_typed[model]['table'][pivot][var]
    data = sorted(probas.items(), reverse=True)
    for proba, data in sorted(probas.items(), reverse=True):
        t = (4-log(proba,2))/3
        print(t)
        plt.plot(data, label=f'table sampling p=1/{proba}', color=((1-t)*1.0+t*186/255, (1-t)*137/255, (1-t)*137/255))
    plt.xlabel('Number of samples')
    plt.ylabel('p-value')
    plt.yscale('log')
    plt.legend()
    plt.savefig('proba.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()

# Saves the graph for the 9-queens, with specific parameters
def save_queens(pivot):
    random = obj_typed['9-queens']['random']
    plt.plot(random, label="RandomVarDom")
    all_vars = obj_typed['9-queens']['table'][pivot]
    datas = [(2,4,all_vars[2][4]), (2,8, all_vars[2][8]), (3,8, all_vars[3][8]), (4,16, all_vars[4][16])]
    for (var, proba, data) in datas:
        plt.plot(data, label=f'table sampling v={var}, p=1/{proba}')
    plt.xlabel('Number of samples')
    plt.ylabel('p-value')
    plt.yscale('log')
    plt.legend()
    plt.savefig('9queens.svg', bbox_inches='tight',pad_inches = 0)
    plt.figure()
    
def main():
    print("Models present in the file :", list(obj.keys()))
    variate_pivot('oc-roster-4s-10d.czn-1', 4, 8)
    variate_var('feature.czn-17738', 2, 2)
    variate_proba('oc-roster-4s-10d.czn-2', 16, 5)
    save_queens(8)

if __name__ == '__main__':
    main()

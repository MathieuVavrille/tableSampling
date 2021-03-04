# Table Sampling

This repository contains the implementation of the table sampling.

## Implementation

All the code is in the folder `src/main/java/org/mvavrill/tableSampling/`. It is divided in subfolders containing different parts of the architecture :
- The folder `models` contains the implementation of different models that can be samples. The models inherit from the abstract class `ModelGenerator`. Most of them are implemented directly in java using choco, but there are classes to import models from flatZinc files `FznLoader` and the specific model from the Megane benchmark `Megane`, associated to the file `csplibmodels/megane.xml`
- The folder `sampler` contains different sampling methods. The main one interesting us is `TableHashDichotomy` that implements the table sampling with the dichotomy adding of tables. `EnumerationSampling` just enumerates the solutions and picks one at random, `RandomStrategySampling` uses the search RandomVarDom.
- The folder `memoizedData` contains classes used by the Jackson library to save data as json
- The folder plotting contains the class `StatisticalTests` used primarily to get the p-value. It also contains different helper functions to convert arrays, and `PythonScriptCreator` contains functions that helps to generate a python script that can be executed to get plots.
The entry point of the code is `TableSampling`, but only contains the CLI definition. The two files containing the codes to test the sampling are `TimeMain` and `PvalueMain`.

At the root of the project is a folder `csplibmodels` that contains models from the csplib, converted in the flatzinc format (.czn). There is also a folder `scripts` containing the python scripts to generate the pvalue graphs or the heat_maps of the times.

## Running the tests

The project uses maven, so all the dependencies should be dealt with automatically. 

Compile the project using `make`. The generated jar file is located in the `target` folder : `tableSampling-1.0-SNAPSHOT-jar-with-dependencies.jar`. To run it simply run `java -jar tableSampling-1.0-SNAPSHOT-jar-with-dependencies` for the default parameters.

The first main parameter is `-pvalue`. If not specified, the code runs running time tests on the megane problem, on call rostering with $obj \le 3$, and the 12-queens. It is possible to choose with `-nb_samples` the number of samples to average the time on (to average the random behaviour). If the parameter `-pvalue` is specified, the code runs tests of the pvalue on the problems of the 8 and 9-queens, on call rostering with $obj \le 1$ and $2$, and feature models. The other parameters have default values, and can be found with the parameter `-help` :
- `-output_file` is the name of the output file. It is a json file, containing either the running times or the list of pvalues. The structure is a map mapping models to the data for the random sampling, and a map mapping pivots, number of variables, and probability to the data for the table sampling with these parameters.
- `-random_seed` the random seed used to initialize the random number generator.
- parameters to do more or less computations. There are lists that are hardcoded in the files `TimeMain` and `PvalueMain` that will define which values of pivot, number of variables and probability to test with. The following parameters allow to only take the first values in this list for the computations.
  - `-nb_pivots` for the pivot
  - `-nb_vars` for the number of variables
  - `-nb_proba` for the probability
All the parameters have aliases that can be found using `-help`.

The output of these computations can be plotted using the python scripts in `scripts`

## Python scripts

There are two python scripts in `scripts` to either process the pvalue or the times. For the times it is possible to generate heat maps, and for the pvalue, it is possible to plot the pvalue after fixing two of the three parameters. It requires at least Python 3.6 to be run.

The first script if this folder is `heat_maps.py`. It takes as parameter the file with the data (output from the java code), and outputs three svg files with the heat maps. To change the parameters, one should modify directly the code, line 69 (last one) to specify different values for the pivot, number of variables or probability fixed when generating the heat maps.


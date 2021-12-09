package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.models.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Random;
import java.util.concurrent.Callable;


/** This is the command line interface to call either the pvalue or the time generation.*/
@Command(name = "tableSampling", mixinStandardHelpOptions = true, version = "tableSampling 4.0",
         description = "Saves the result of either pvalue or time for the sampling of different problems")
public class TableSampling implements Callable<Integer> {

  @Option(names = {"-pvalue", "-p"}, description = "run the p-value samplings. Will run time sampling if not specified.")
  private boolean pvalue = false;

  @Option(names = {"-nb_samples", "-s"}, description = "The number of samples to average on when testing the running time. Useless to specify it when getting the p-value results. Default is the average over 50 samples")
  private int nbSamples = 50;

  @Option(names = {"-random_seed", "-r"}, description = "Random seed to use to initialize the random number generator. Default is 97")
  private Long seed = 97L;

  @Option(names = {"-output_file", "-o"}, description = "Name of the output file. If the file already exists the already computed results will not be computed again, and the file will be appended with new results. Default is saved close to the model (either model-pvalue.json or model-times.json)")
  private String fileName = "";

  @Option(names = {"-nb_pivots", "-npi"}, description = "The number of pivots to run. Will run the tests for the n first pivots from 2,4,8,16,32,64. Default will run pivot = 2,4,8")
  private int nbPivots = 3;

  @Option(names = {"-nb_vars", "-nv"}, description = "The number of variables to run. Will run the tests for the n first number of variables from 2,3,4,5,6. Default will run v = 2,3,4.")
  private int nbVars = 3;

  @Option(names = {"-nb_proba", "-npr"}, description = "The number of probabilities to run. Will run the tests for the n first probabilities from 1/2,1/4,1/8,1/16,1/32,1/64,1/128,1/256. Default will run p = 1/2, 1/4, 1/8")
  private int nbProbas = 3;

  @Option(names = {"-time_limit", "-t"}, description = "The time limit (in seconds) for a single experiment. If the time limit is met the other experiments (with different random seeds) will not be done. Default is one second")
  private long timeLimit = 1L;

  @Option(names = {"-instance", "-i"}, description = "The instance to run. Can be a flatzinc file (then you should input the path), or it can be 'megane' or 'queens-[n]' where you can replace [n] with a value")
  private String instanceName = "queens-9";

  @Option(names = {"-do_dichotomy", "-dd"}, description = "Run the dichotomy table sampling additionnally. Default will not run it.")
  private boolean doDichotomy = false;
  
  @Option(names = {"-do_lin_mod_eq", "-dl"}, description = "Run the sampling with the linear modular equality system. Default will not run it.")
  private boolean doHashMod = false;
  
  @Option(names = {"-dnp"}, description = "Run the Table Sampling without propagation (for benchmark purpose). Default will not run it.")
  private boolean noPropagate = false;

  @Override
  public Integer call() throws Exception {
    if (nbPivots > 6)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different pivots. pivot should be <= 6.");
    if (nbVars > 5)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different number of variables. nb_vars should be <= 4.");
    if (nbProbas > 8)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different probabilities. nb_proba should be <= 8.");

    final Random random = new Random(seed);

    ModelGenerator modGen = modelToSolve();
    if (fileName.equals(""))
      fileName = getOutName();
    
    if (pvalue)
      new PvalueMain(modGen, fileName, nbPivots, nbVars, nbProbas, timeLimit*1000000000L, doHashMod, random).exec();
    else
      new TimeMain(modGen, fileName, nbPivots, nbVars, nbProbas, nbSamples, timeLimit*1000000000L, doDichotomy, doHashMod, noPropagate, random).exec();
    return 0;
  }

  private ModelGenerator modelToSolve() {
    if (instanceName.substring(0,6).equals("queens"))
      return new NQueens(Integer.parseInt(instanceName.substring(7,instanceName.length())));
    if (instanceName.equals("megane"))
      return new Megane("truc");
    return new FznLoader(instanceName);
  }
  
  private String getOutName() {
    if (instanceName.substring(0,6).equals("queens"))
      return "queens-"+Integer.parseInt(instanceName.substring(7,instanceName.length()))+"-"+solvingType()+".json";
    if (instanceName.equals("megane"))
      return "megane-"+solvingType()+".json";
    return FznLoader.appendName(instanceName, solvingType());
  }

  private String solvingType() {
    return pvalue ? "pvalue" : "time";
  }

  public static void main(String... args) {
    int exitCode = new CommandLine(new TableSampling()).execute(args);
    System.exit(exitCode);
  }
}

package org.mvavrill.tableSampling;

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

  @Option(names = {"-output_file", "-o"}, description = "Name of the output file. If the file already exists the already computed results will not be computed again, and the file will be appended with new results. Default is 'sampling_out.json'")
  private String fileName = "sampling_out.json";

  @Option(names = {"-nb_pivots", "-npi"}, description = "The number of pivots to run. Will run the tests for the n first pivots from 2,4,8,16,36,64. Default will run pivot = 2,4,8")
  private int nbPivots = 3;

  @Option(names = {"-nb_vars", "-nv"}, description = "The number of variables to run. Will run the tests for the n first number of variables from 2,3,4,5. Default will run v = 2,3,4.")
  private int nbVars = 3;

  @Option(names = {"-nb_proba", "-npr"}, description = "The number of probabilities to run. Will run the tests for the n first probabilities from 1/2,1/4,1/8,1/16,1/32,1/64,1/128,1/256. Default will run p = 1/2, 1/4, 1/8")
  private int nbProbas = 3;

  @Override
  public Integer call() throws Exception {
    if (nbPivots > 6)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different pivots. pivot should be <= 6.");
    if (nbVars > 4)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different number of variables. nb_vars should be <= 4.");
    if (nbProbas > 8)
      throw new IllegalArgumentException("It is not possible to run with more than 6 different probabilities. nb_proba should be <= 8.");

    final Random random = new Random(seed);
    
    if (pvalue)
      new PvalueMain(fileName, nbPivots, nbVars, nbProbas, random).exec();
    else
      new TimeMain(fileName, nbPivots, nbVars, nbProbas, nbSamples, random).exec();
    return 0;
  }

  public static void main(String... args) {
    int exitCode = new CommandLine(new TableSampling()).execute(args);
    System.exit(exitCode);
  }
}

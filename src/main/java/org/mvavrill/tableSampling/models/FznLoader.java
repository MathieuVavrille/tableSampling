package org.mvavrill.tableSampling.models;

import org.mvavrill.tableSampling.zpz.ZpZ;
import org.mvavrill.tableSampling.memoizedData.*;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.parser.flatzinc.Flatzinc;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.javatuples.Triplet;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * A class allowing to load a fzn or czn file and generate the model from it
 * Uses the chocoparser classes
 */
public class FznLoader extends OptimisationModelGenerator {

  private final String fileName;
  private final MemoizedBound data;
  private final ZpZ zpz;

  public FznLoader(final String fileName) {
    this(fileName, 0);
  }

  public FznLoader(final String fileName, final int dataId) {
    this(fileName, dataName(fileName), dataId);
  }

  public FznLoader(final String fileName, final String fileNameData, final int dataId) {
    //org.chocosolver.parser.RegParser.PRINT_LOG = false;
    this.fileName = fileName;
    if (dataId < 0)
      this.data = null;
    else
      this.data = getMemoizedBound(fileNameData, dataId);
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  public static String appendName(final String fileName, final String append) {
    return fileName.substring(0, fileName.length()-4)+"-"+append+".json";
  }
  
  public static String dataName(final String fileName) {
    return appendName(fileName, "data");
  }
  
  public static String pvalueName(final String fileName) {
    return appendName(fileName, "pvalue");
  }
  
  public static String timeName(final String fileName) {
    return appendName(fileName, "time");
  }

  private static MemoizedBound getMemoizedBound(final String fileName, final int dataId) {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        List<MemoizedBound> memData = mapper.readValue(file, MemoizedData.class).getBounds();
        return (memData.size() == 0) ? null : memData.get(dataId);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    return null;
  }

  
  @Override
  public ModelAndVars generateModelAndVars() {
    Flatzinc fzn = new Flatzinc() { // Fix for the memory leak problem. Close the file after reading
        @Override
        public void buildModel() {
          Runtime.getRuntime().removeShutdownHook(statOnKill);
          super.buildModel();
        }
      };
    //fzn.addListener(new BaseFlatzincListener(fzn));
    try {
      if(fzn.setUp(new String[]{fileName})) {
        fzn.getSettings();
        fzn.createSolver();
        fzn.buildModel();
        fzn.configureSearch();
      }
      Model model = fzn.getModel();
      IntVar[] vars = model.retrieveIntVars(true);
      if (data != null)
        switch (model.getResolutionPolicy()) {
        case MAXIMIZE:
          IntVar obj = model.getObjective().asIntVar();
          obj.ge(data.getBound()).post();
          model.clearObjective();
          List<IntVar> new_vars = new ArrayList<IntVar>();
          for (IntVar var : vars)
            if (var != obj)
              new_vars.add(var);
              vars = new_vars.toArray(new IntVar[0]);
          break;
        case MINIMIZE:
          obj = model.getObjective().asIntVar();
          obj.le(data.getBound()).post();
          model.clearObjective();
          new_vars = new ArrayList<IntVar>();
          for (IntVar var : vars)
            if (var != obj)
              new_vars.add(var);
              vars = new_vars.toArray(new IntVar[0]);
          break;
        }
      return new ModelAndVars(model, vars);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null; // Can't happen because of error catch
  }

  
  @Override
  public Triplet<ModelAndVars,IntVar,Boolean> generateModelVarsObj() { // Returns the satisfaction problem with the objective
    Flatzinc fzn = new Flatzinc() { // Fix for the memory leak problem. Close the file after reading
        @Override
        public void buildModel() {
          Runtime.getRuntime().removeShutdownHook(statOnKill);
          super.buildModel();
        }
      };
    //fzn.addListener(new BaseFlatzincListener(fzn));
    try {
      if(fzn.setUp(new String[]{fileName})) {
        fzn.getSettings();
        fzn.createSolver();
        fzn.buildModel();
        fzn.configureSearch();
      }
      Model model = fzn.getModel();
      IntVar[] vars = model.retrieveIntVars(true);
      IntVar obj;
      List<IntVar> new_vars;
      if (data != null)
        switch (model.getResolutionPolicy()) {
        case MAXIMIZE:
          obj = model.getObjective().asIntVar();
          new_vars = new ArrayList<IntVar>();
          for (IntVar var : vars)
            if (var != obj)
              new_vars.add(var);
          vars = new_vars.toArray(new IntVar[0]);
          return new Triplet<ModelAndVars,IntVar,Boolean>(new ModelAndVars(model, vars), obj, true);
        case MINIMIZE:
          obj = model.getObjective().asIntVar();
          new_vars = new ArrayList<IntVar>();
          for (IntVar var : vars)
            if (var != obj)
              new_vars.add(var);
          vars = new_vars.toArray(new IntVar[0]);
          return new Triplet<ModelAndVars,IntVar,Boolean>(new ModelAndVars(model, vars), obj, false);
        default:
          throw new IllegalStateException("You cannot ask for objective on a satisfaction problem");
        }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null; // Can't happen because of error catch
  }

  @Override
  public String getName() {
    String[] path = fileName.split("/");
    return path[path.length-1] + "-" + Integer.toString(data.getBound());
  }
  
  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
}

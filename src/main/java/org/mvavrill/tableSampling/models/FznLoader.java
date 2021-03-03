package org.mvavrill.tableSampling.models;

import org.mvavrill.tableSampling.memoizedData.MemoizedData;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.parser.flatzinc.Flatzinc;
import org.chocosolver.parser.flatzinc.BaseFlatzincListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * A class allowing to load a fzn or czn file and generate the model from it
 * Uses the chocoparser classes
 */
public class FznLoader extends ModelGenerator {

  private final String fileName;
  private final MemoizedData data;

  public FznLoader(final String fileName) {
    this(fileName, 0);
  }

  public FznLoader(final String fileName, final int dataId) {
    this(fileName, dataName(fileName), dataId);
  }

  public FznLoader(final String fileName, final String fileNameData, final int dataId) {
    org.chocosolver.parser.RegParser.PRINT_LOG = false;
    this.fileName = fileName;
    if (dataId < 0)
      this.data = null;
    else
      this.data = getMemoizedData(fileNameData, dataId);
  }
  
  public static String dataName(final String fileName) {
    return fileName.substring(0, fileName.length()-4)+"-data.json";
  }

  private static MemoizedData getMemoizedData(final String fileName, final int dataId) {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, MemoizedData[].class)[dataId];
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
    fzn.addListener(new BaseFlatzincListener(fzn));
    try {
      if(fzn.setUp(new String[]{fileName})) {
        fzn.getSettings();
        fzn.createSolver();
        fzn.buildModel();
        fzn.configureSearch();
      }
      Model model = fzn.getModel();
      if (data != null)
        switch (model.getResolutionPolicy()) {
        case MAXIMIZE:
          model.getObjective().asIntVar().ge(data.getBound()).post();
          model.clearObjective();
          break;
        case MINIMIZE:
          model.getObjective().asIntVar().le(data.getBound()).post();
          model.clearObjective();
          break;
        }
      //model.getSolver().getSearch()....
      return new ModelAndVars(model, model.retrieveIntVars(true));
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
}

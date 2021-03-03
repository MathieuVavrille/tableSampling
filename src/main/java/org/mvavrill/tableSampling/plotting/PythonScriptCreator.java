package org.mvavrill.tableSampling.plotting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;

public class PythonScriptCreator {
  final FileWriter myWriter;
  
  
  public PythonScriptCreator(final String fileName) throws IOException {
    myWriter = new FileWriter(fileName);
    myWriter.write("from matplotlib import pyplot as plt\n\nplt.figure()\n");
  }

  public void addPlot(final String label, final List<String> values, final boolean isLine, final int markersize) throws IOException {
    myWriter.write("plt.plot([" + String.join(",", values) + "], " + ((!isLine) ? "'o', markersize = " + markersize + ", " : "") + "label='" + label + "')\n");
  }
  
  public void addPlot(final String label, final List<String> coordinates, final List<String> values, final boolean isLine, final int markersize) throws IOException {
    myWriter.write("plt.plot([" + String.join(",", coordinates) + "], [" + String.join(",", values) + "], " + ((!isLine) ? "'o', markersize = " + markersize + ", " : "") + "label='" + label + "')\n");
  }

  public void addVerticalLine(final String name, final int value) throws IOException {
    myWriter.write("plt.axvline(x=" + value + ", label='" + name + "=" + value + "', linestyle='--')\n");
  }

  public void saveFigure(final String title, final String xAxis, final String yAxis, final boolean isLogScale, final String fileName) throws IOException {
    myWriter.write("plt.title('" + title + "')\nplt.xlabel('" + xAxis + "')\nplt.ylabel('" + yAxis + "')\n" + (isLogScale ? "plt.yscale('log')" : "") + "\nplt.legend()\nplt.savefig('" + fileName + "')\n\nplt.figure()\n");
  }

  public void close() throws IOException {
    myWriter.close();
  }
}

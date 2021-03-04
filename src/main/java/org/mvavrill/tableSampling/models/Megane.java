package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.BoolVar;

import org.javatuples.Pair;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;


/**
 * The model generator for the problem of the configurations of the Renault Mégane
 */
public class Megane extends ModelGenerator {

  private final Map<Integer, Pair<Integer,String>> variables;
  private final List<Pair<Tuples, List<Integer>> > tables;

  public Megane() {
    File inputFile = new File("csplibmodels/megane.xml");
    Element mainXmlElement = getMainXmlElement(inputFile);
    // Domains
    final NodeList xmlDomains = ((Element) mainXmlElement.getElementsByTagName("domains").item(0)).getElementsByTagName("domain");
    Map<String, Pair<Integer, String>> domains = new HashMap<String, Pair<Integer,String>>();
    for (int domainId = 0; domainId < xmlDomains.getLength(); domainId++) {
      Element xmlDomain = (Element) xmlDomains.item(domainId);
      domains.put(xmlDomain.getAttribute("name"), new Pair<Integer,String>(Integer.parseInt(xmlDomain.getAttribute("nbValues")), xmlDomain.getTextContent()));
    }
    // Variables
    final NodeList xmlVariables = ((Element) mainXmlElement.getElementsByTagName("variables").item(0)).getElementsByTagName("variable");
    variables = new HashMap<Integer, Pair<Integer,String>>();
    for (int variableId = 0; variableId < xmlVariables.getLength(); variableId++) {
      Element xmlVariable = (Element) xmlVariables.item(variableId);
      variables.put(Integer.parseInt(xmlVariable.getAttribute("name")), domains.get(xmlVariable.getAttribute("domain")));
    }
    // Tuples
    final NodeList xmlAllTuples = ((Element) mainXmlElement.getElementsByTagName("relations").item(0)).getElementsByTagName("relation");
    HashMap<String, Tuples> allTuples = new HashMap<String, Tuples>();
    for (int tupleId = 0; tupleId < xmlAllTuples.getLength(); tupleId++) {
      Element xmlTuples = (Element) xmlAllTuples.item(tupleId);
      allTuples.put(xmlTuples.getAttribute("name"), getTuplesFromString(xmlTuples.getTextContent(), xmlTuples.getAttribute("semantics") == "supports", Integer.parseInt(xmlTuples.getAttribute("arity"))));
    }
    // Tables
    final NodeList xmlTables = ((Element) mainXmlElement.getElementsByTagName("constraints").item(0)).getElementsByTagName("constraint");
    tables = new ArrayList<Pair<Tuples, List<Integer>>>();
    for (int tableId = 0; tableId < xmlTables.getLength(); tableId++) {
      Element xmlTable = (Element) xmlTables.item(tableId);
      tables.add(new Pair<Tuples, List<Integer>>(allTuples.get(xmlTable.getAttribute("reference")), Arrays.stream(xmlTable.getAttribute("scope").split(" ")).map(i -> Integer.parseInt(i)).collect(Collectors.toList())));
    }
  }

  private Element getMainXmlElement(final File file) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
      doc.getDocumentElement().normalize();
      return doc.getDocumentElement();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null; // Should not happen because of the catch
  }

  private Tuples getTuplesFromString(String tuplesString, final boolean isSupports, final int arity) {
    Tuples allowedTuples = new Tuples(isSupports);
    if (tuplesString == "")
      return allowedTuples;
    String[] splittedTable = tuplesString.split("\\|");
    for (String tplStr : splittedTable) {
      int[] tplInt = Arrays.stream(tplStr.split(" "))
        .mapToInt(iStr -> Integer.parseInt(iStr))
        .toArray();
      allowedTuples.add(tplInt);
    }
    return allowedTuples;
  }
    

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model();
    Map<Integer, IntVar> intVars = variables.entrySet().stream()
    .collect(Collectors.toMap(Map.Entry::getKey, es -> generateIntVar(model, es.getValue().getValue0(), es.getValue().getValue1(), "x" + es.getKey())));
    tables.stream()
    .forEach(p -> model.table(p.getValue1().stream().map(i -> intVars.get(i)).toArray(IntVar[]::new), p.getValue0()).post());
    return new ModelAndVars(model, intVars.entrySet().stream().map(Map.Entry::getValue).toArray(IntVar[]::new));
  }

  private IntVar generateIntVar(final Model model, final int domainSize, final String domainRepr, final String name) {
    if (domainSize == 1)
      return model.intVar(Integer.parseInt(domainRepr));
    String[] boundsStr = domainRepr.split("\\.\\.");
    int lb = Integer.parseInt(boundsStr[0]);
    int ub = Integer.parseInt(boundsStr[1]);
    if (ub == 0 && lb == 1)
      return model.boolVar(name);
    else
      return model.intVar(name, lb, ub);
  }

  @Override
  public String getName() {
    return "Megane";
  }
}

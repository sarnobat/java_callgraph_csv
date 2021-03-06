package com.rohidekar.callgraph.calls;

import com.rohidekar.callgraph.common.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Transforms relationships into graphs
 */
public class RelationshipToGraphTransformerCallHierarchy {

  public static void printCallGraph(Relationships relationships) {
    Map<String, GraphNode> allMethodNamesToMethodNodes = RelationshipToGraphTransformerCallHierarchy
        .determineCallHierarchy(relationships);
    relationships.validate();
    Set<GraphNode> rootMethodNodes = RelationshipToGraphTransformerCallHierarchy
        .findRootCallers(allMethodNamesToMethodNodes);
    if (rootMethodNodes.size() < 1) {
    		System.err.println("ERROR: no root nodes to print call tree from."); 
    }
    TreePrinterCalls.printTrees(relationships, rootMethodNodes);
  }

  private static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
    Set<GraphNode> rootMethodNodes;
    rootMethodNodes = new HashSet<GraphNode>();
    for (GraphNode aNode : allMethodNamesToMethods.values()) {
      Set<GraphNode> roots = new HashSet<GraphNode>();
      RootsVisitor rootsVisitor = new RootsVisitor();
      RootFinder.getRoots(aNode, roots, rootsVisitor);
      rootMethodNodes.addAll(roots);
    }
    return rootMethodNodes;
  }

  private static Map<String, GraphNode> determineCallHierarchy(Relationships relationships) {
    relationships.validate();
    Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
    // Create a custom call graph structure from the multimap (flatten)
    for (String parentMethodNameKey : relationships.getAllMethodCallers()) {
      System.err
          .println("RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - " + parentMethodNameKey);
      if (Ignorer.shouldIgnore(parentMethodNameKey)) {
        continue;
      }
      GraphNodeInstruction parentEnd = (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
      if (parentEnd == null) {
        MyInstruction parentMethodInstruction = relationships.getMethod(parentMethodNameKey);
        if (parentMethodInstruction == null) {
          System.err.println(
              "RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - WARNING: couldn't find instruction for  "
                  + parentMethodNameKey);
          //continue;
          System.exit(-1);
        }
        parentEnd = new GraphNodeInstruction(parentMethodInstruction);
        allMethodNamesToMethods.put(parentMethodNameKey, parentEnd);
        if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
          throw new IllegalAccessError("determineCallHierarchy() 1 ");
        }
      }
      if (parentEnd.toString().contains("Millis") && parentMethodNameKey.contains("Repository")) {
    	  	throw new IllegalAccessError("determineCallHierarchy() 2 ");
      }
      Collection<MyInstruction> calledMethods = relationships.getCalledMethods(parentMethodNameKey);
      for (MyInstruction childMethod : calledMethods) {
        if (Ignorer.shouldIgnore(childMethod.getMethodNameQualified())) {
          continue;
        }
        System.err.println("RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - -> "
            + childMethod.getMethodNameQualified());
        GraphNodeInstruction child = (GraphNodeInstruction) allMethodNamesToMethods
            .get(childMethod.getMethodNameQualified());
        if (child == null) {
          child = new GraphNodeInstruction(childMethod);
          allMethodNamesToMethods.put(childMethod.getMethodNameQualified(), child);
        }
        parentEnd.addChild(child);
        child.addParent(parentEnd);
      }
    }
    relationships.validate();
    return allMethodNamesToMethods;
  }
}

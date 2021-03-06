package com.rohidekar.callgraph.calls;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rohidekar.callgraph.Main;
import com.rohidekar.callgraph.Main2018;
import com.rohidekar.callgraph.common.*;
import com.rohidekar.callgraph.containments.TreeDepthCalculator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreeModel;

/**
 * Transforms relationships into graphs
 */
public class RelationshipToGraphTransformerCallHierarchyV2 {

	public static void printTreeTest(GraphNode tn, int level, Set<GraphNode> visited) {
		if (visited.contains(tn)) {
			return;
		}
		visited.add(tn);
		if (((MyInstruction) tn.getSource()).getMethodNameQualified()
				.equals("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
			throw new IllegalAccessError("printTreeTest");
		}
		for (GraphNode child : tn.getChildren()) {
			System.out.println("\"" + tn.toString() + "\",\"" + child.toString() + "\"");
			printTreeTest(child, level + 1, visited);
		}

	}

	public static Set<GraphNode> findRootCallers(Map<String, GraphNode> allMethodNamesToMethods) {
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

	public static Map<String, GraphNode> determineCallHierarchy(RelationshipsV2 relationships, Collection<String> allMethodCallers, Map<String, MyInstruction> allMethodNamesToMyInstructions) {
		Map<String, GraphNode> allMethodNamesToMethods = new LinkedHashMap<String, GraphNode>();
		// Create a custom call graph structure from the multimap (flatten)
		for (String parentMethodNameKey : allMethodCallers) {
			System.err.println(
					"RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - " + parentMethodNameKey);
			if (Ignorer.shouldIgnore(parentMethodNameKey)) {
				continue;
			}
			GraphNodeInstruction parentEnd = (GraphNodeInstruction) allMethodNamesToMethods.get(parentMethodNameKey);
			if (parentEnd == null) {
				MyInstruction parentMethodInstruction = allMethodNamesToMyInstructions.get(parentMethodNameKey);
				if (parentMethodInstruction == null) {
					System.err.println(
							"RelationshipToGraphTransformerCallHierarchy.determineCallHierarchy() - WARNING: couldn't find instruction for  "
									+ parentMethodNameKey);
					// continue;
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

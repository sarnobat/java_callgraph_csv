// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;

import com.google.common.collect.ImmutableMap;
import com.rohidekar.callgraph.calls.RelationshipToGraphTransformerCallHierarchy;
import com.rohidekar.callgraph.calls.RelationshipToGraphTransformerCallHierarchyV2;
import com.rohidekar.callgraph.common.GraphNode;
import com.rohidekar.callgraph.common.Relationships;
import com.rohidekar.callgraph.common.RelationshipsV2;

/**
 * The previous version is too complicated to understand so I couldn't
 * troubleshoot issues (e.g. why is syncTestsAndSettings not showing up in the
 * csv).
 * 
 * It's best to rewrite the app more simply, as a series of pipeline programs (I
 * hope!).
 * 
 * Ideally this should be a separate maven project so we can prune all the
 * unneeded classes, but that's counterproductive in rapidly developing this
 * version.
 * 
 * put -Xmx1024m in the VM args
 *
 * @author ss401533@gmail.com (Sridhar Sarnobat)
 *
 *         2018-12
 */
public class Main2018 {

	public static final int MIN_TREE_DEPTH = 1;
	public static int MAX_TREE_DEPTH = 187;// 27 works, 30 breaks
	// Only print from roots this far below the top level package that contains
	// classes
	public static final int ROOT_DEPTH = 27;

	public static final String[] substringsToIgnore = { "java", "Logger", ".toString", "Exception", };

	public static void main(String[] args) {
		String resource;
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException();
		} else {
			resource = args[0];
		}
		printGraphs(resource);
		System.err.println("Now use d3_helloworld_csv.git/singlefile_automated/ for visualization. For example: ");
		System.err.println("  cat /tmp/calls.csv | sh csv2d3.sh | tee /tmp/index.html");
	}

	private static void printGraphs(String classDirOrJar) {
		RelationshipsV2 relationships = RelationshipsV2.relationshipsV2(classDirOrJar);

		relationships.validate();
		Map<String, GraphNode> allMethodNamesToMethodNodes = RelationshipToGraphTransformerCallHierarchyV2
				.determineCallHierarchy(relationships, relationships.getAllMethodCallers());
		relationships.validate();
		Set<GraphNode> rootMethodNodes = RelationshipToGraphTransformerCallHierarchyV2
				.findRootCallers(allMethodNamesToMethodNodes);
		if (rootMethodNodes.size() < 1) {
			System.err.println("ERROR: no root nodes to print call tree from.");
		}
		RelationshipToGraphTransformerCallHierarchyV2.printTrees(relationships, rootMethodNodes);
	}
}

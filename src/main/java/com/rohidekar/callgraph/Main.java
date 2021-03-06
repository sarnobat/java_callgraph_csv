// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph;

import com.rohidekar.callgraph.calls.RelationshipToGraphTransformerCallHierarchy;
import com.rohidekar.callgraph.common.Relationships;
import com.rohidekar.callgraph.containments.RelationshipToGraphTransformerContainments;
import com.rohidekar.callgraph.packages.RelationshipToGraphTransformerPackages;

/**
 * put -Xmx1024m in the VM args
 *
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 *
 * 2016-12
 */
@Deprecated // Not finding syncTestsAndSettings, too complex to debug and fix
public class Main {
//  private static Logger log = Logger.getLogger(Main.class);
  // TODO (feature):add call relationship between 
  // TODO (feature): build containment hierarchy using import statements
  // TODO(feature): Order call graphs by depth
  // EmployeeAssembler
  // TODO(testing): make printGraphs return a string buffer so you can do assertions in unit tests

  // TODO: Create a configuration object. Have several profiles: automatic, maximal
  private static final boolean PRINT_CONTAINMENT = true;
  private static final boolean PRINT_CALL_TREE = true;
  private static final boolean PRINT_PACKAGE_ARCHITECTURE = true;


  public static final int MIN_TREE_DEPTH = 1;
  public static int MAX_TREE_DEPTH = 187;// 27 works, 30 breaks
  // Only print from roots this far below the top level package that contains classes
  public static final int ROOT_DEPTH = 27;

  public static final String[] substringsToIgnore = {"java", "Logger", ".toString", "Exception",
      };


  public static void main(String[] args) {
    String resource;
    if (args == null || args.length < 1) {
//       resource = "/Users/ssarnobat/work/src/saas/services/subscriber";
//		resource = "/Users/ssarnobat/work/src/saas/services/plancycle/target";
//		resource = "/Users/ssarnobat/work/src/saas/";
//resource = "/Users/ssarnobat/Desktop/work/src/webservices/rms-plugin/rms-core/";
resource = "/Users/ssarnobat/Desktop/work/src/webservices/rms-plugin/rms-services/";
//       resource = "/Users/ssarnobat/github/nanohttpd/target";
      //resource = "/Users/ssarnobat/github/java_callgraph_csv/target";
      // TODO: use the current working directory as the class folder, not
      // an arbitrary jar
    } else {
      resource = args[0];
    }
    printGraphs(resource);
    System.err.println("Now use d3_helloworld_csv.git/singlefile_automated/ for visualization. For example: ");
    System.err.println("  cat /tmp/calls.csv | sh csv2d3.sh | tee /tmp/index.html");
  }

  private static void printGraphs(String classDirOrJar) {
    Relationships relationships = new Relationships(classDirOrJar);
    relationships.validate();
    if (PRINT_CALL_TREE) {
      RelationshipToGraphTransformerCallHierarchy.printCallGraph(relationships);
    }
    if (PRINT_PACKAGE_ARCHITECTURE) {
//      RelationshipToGraphTransformerPackages.printPackages(relationships);
    }
    System.err.println("Containment Hierarchy");
    if (PRINT_CONTAINMENT) {
//      RelationshipToGraphTransformerContainments.printContainment(relationships);
    }
  }
}

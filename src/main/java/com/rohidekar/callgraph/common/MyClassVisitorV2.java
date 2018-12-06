package com.rohidekar.callgraph.common;

import com.google.common.collect.Lists;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import gr.gousiosg.javacg.stat.ClassVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MyClassVisitorV2 extends ClassVisitor {

  private JavaClass classToVisit;
  private RelationshipsV2 relationships;

  private Map<String, JavaClass> visitedClasses = new HashMap<String, JavaClass>();

  public MyClassVisitorV2(JavaClass classToVisit, RelationshipsV2 relationships) {
    super(classToVisit);
    this.classToVisit = classToVisit;
    relationships.addPackageOf(classToVisit);
    this.relationships = relationships;
  }

  public void setVisited(JavaClass javaClass) {
    this.visitedClasses.put(javaClass.getClassName(), javaClass);
  }

  public boolean isVisited(JavaClass javaClass) {
    return this.visitedClasses.values().contains(javaClass);
  }

  @Override
  public void visitJavaClass(JavaClass javaClass) {
    if (this.isVisited(javaClass)) {
      return;
    }
    this.setVisited(javaClass);
    if (javaClass.getClassName().equals("java.lang.Object")) {
      return;
    }
    if (Ignorer.shouldIgnore(javaClass)) {
      return;
    }
    relationships.addPackageOf(javaClass);
    relationships.updateMinPackageDepth(javaClass);

    // Parent classes
    List<String> parentClasses = getInterfacesAndSuperClasses(javaClass);
    for (String anInterfaceName : parentClasses) {
      if (Ignorer.shouldIgnore(anInterfaceName)) {
        continue;
      }
      JavaClass anInterface = relationships.getClassDef(anInterfaceName);
      if (anInterface == null) {
        relationships.deferParentContainment(anInterfaceName, javaClass);
        relationships.addContainmentRelationshipStringOnly(
            anInterfaceName, classToVisit.getClassName());
      } else {
        relationships.addContainmentRelationship(anInterface.getClassName(), classToVisit);
      }
    }
    // Methods
    for (Method method : javaClass.getMethods()) {
      method.accept(this);
    }
    // fields
    Field[] fs = javaClass.getFields();
    for (Field f : fs) {
      f.accept(this);
    }
  }

  public static List<String> getInterfacesAndSuperClasses(JavaClass javaClass) {
    List<String> parentClasses =
        Lists.asList(javaClass.getSuperclassName(), javaClass.getInterfaceNames());
    return parentClasses;
  }

  @Override
  public void visitMethod(Method method) {
    String className = classToVisit.getClassName();
    ConstantPoolGen classConstants = new ConstantPoolGen(classToVisit.getConstantPool());
    MethodGen methodGen = new MethodGen(method, className, classConstants);
    new MyMethodVisitorV2(methodGen, classToVisit, relationships).start();
  }

  @Override
  public void visitField(Field field) {
    Type fieldType = field.getType();
    if (fieldType instanceof ObjectType) {
      ObjectType objectType = (ObjectType) fieldType;
      addContainmentRelationship(this.classToVisit, objectType.getClassName(), relationships, true);
    }
  }

  public static void addContainmentRelationship(JavaClass parentClassToVisit,
      String childClassNameQualified, RelationshipsV2 relationships, boolean allowDeferral) {
    if (Ignorer.shouldIgnore(childClassNameQualified)) {
      return;
    }
    JavaClass childClass = null;
    try {
      childClass = Repository.lookupClass(childClassNameQualified);
      System.err.println("SRIDHAR MyClassVisitor.addContainmentRelationship() - this NEVER works");
      System.exit(-1);
    } catch (ClassNotFoundException e) {
      
        System.err.println(e);
      if (allowDeferral) {
        relationships.deferContainmentVisit(parentClassToVisit, childClassNameQualified);
		System.err.println("SRIDHAR MyClassVisitor.addContainmentRelationship() - delay determining containment of " + childClassNameQualified);
      } else {
        childClass = relationships.getClassDef(childClassNameQualified);
        if (childClass == null) {
          if (!Ignorer.shouldIgnore(childClassNameQualified)) {
            System.err.println("WARN: Still can't find " + childClassNameQualified);
          }
        } else {
        		System.err.println("SRIDHAR MyClassVisitor.addContainmentRelationship() - finally were able to determine containment: " + parentClassToVisit.getClassName() +" is a parent class of " + childClass.getClassName());
        }
      }
    }
    if (childClass == null) {
      System.err.println("WARN: Couldn't find " + childClassNameQualified);
    } else {
    	// TODO: this is bad. We are mutating an input parameter. No wonder this code is so hard to understand
      relationships.addContainmentRelationship(parentClassToVisit.getClassName(), childClass);
    }
  }
}

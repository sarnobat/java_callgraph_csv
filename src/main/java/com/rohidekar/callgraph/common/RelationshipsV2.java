package com.rohidekar.callgraph.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// This class is too complicated
// (leave it for the 2016 version, do not use it in the 2018 one). It's too
// object oriented, and I know in 2018-12 that OO is terrible.
public class RelationshipsV2 {

	// The top level package with classes in it
	int minPackageDepth = Integer.MAX_VALUE;

	// Relationships
	private Multimap<String, MyInstruction> callingMethodToMethodInvocationMultiMap = LinkedHashMultimap.create();
	private Multimap<String, JavaClass> classNameToFieldTypesMultiMap = LinkedHashMultimap.create();
	private Multimap<String, String> classNameToFieldTypeNamesMultiMap = LinkedHashMultimap.create();
	private Multimap<String, String> parentPackageNameToChildPackageNameMultiMap = LinkedHashMultimap.create();

	// Name to Value mappings
	private Map<String, MyInstruction> allMethodNameToMyInstructionMap = new HashMap<String, MyInstruction>();

	// nodes
	private ImmutableMap<String, JavaClass> classNameToJavaClassMap;

	// Objects that cannot yet be found
	private Set<DeferredChildContainment> deferredChildContainments = new HashSet<DeferredChildContainment>();
	private Set<DeferredSuperMethod> deferredSuperMethod = new HashSet<DeferredSuperMethod>();
	private Set<DeferredParentContainment> deferredParentContainments = new HashSet<DeferredParentContainment>();

	private Set<String> classNames = new HashSet<String>();

	@Deprecated
	public RelationshipsV2(String resource) {
		System.err.println("SRIDHAR Relationships.Relationships() - visiting classes in " + resource);
		Map<String, JavaClass> javaClasses = getJavaClassesFromResource(resource);
		this.classNameToJavaClassMap = ImmutableMap.copyOf(javaClasses);
		for (JavaClass jc : this.classNameToJavaClassMap.values()) {
			visitJavaClass(jc, this);
		}

		// These deferred relationships should not be necessary, but if you debug them
		// you'll see that they find additional relationships.
		DeferredRelationshipsV2.handleDeferredRelationships(this);
	}
	
	private RelationshipsV2() {
		
	}

	public static RelationshipsV2 relationshipsV2(String resource) {
		System.err.println("SRIDHAR Relationships.Relationships() - visiting classes in " + resource);
		Map<String, JavaClass> javaClasses = getJavaClassesFromResource(resource);
		RelationshipsV2 r = new RelationshipsV2();
		r.classNameToJavaClassMap = ImmutableMap.copyOf(javaClasses);
		for (JavaClass jc : r.classNameToJavaClassMap.values()) {
			visitJavaClass(jc, r);
		}

		// These deferred relationships should not be necessary, but if you debug them
		// you'll see that they find additional relationships.
		DeferredRelationshipsV2.handleDeferredRelationships(r);
		return new RelationshipsV2();
	}
	public static Map<String, JavaClass> getJavaClassesFromResource(String resource) {
		Map<String, JavaClass> javaClasses = new HashMap<String, JavaClass>();
		boolean isJar = resource.endsWith("jar");
		if (isJar) {
			String zipFile = null;
			zipFile = resource;
			File jarFile = new File(resource);
			if (!jarFile.exists()) {
				System.out.println("JavaClassGenerator.getJavaClassesFromResource(): WARN: Jar file " + resource
						+ " does not exist");
			}
			Collection<JarEntry> entries = null;
			try {
				entries = Collections.list(new JarFile(jarFile).entries());
			} catch (IOException e) {
				System.err.println("JavaClassGenerator.getJavaClassesFromResource() - " + e);
				System.exit(-1);
			}
			if (entries == null) {
				System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No entry");
				return javaClasses;
			}
			for (JarEntry entry : entries) {
				if (entry.isDirectory()) {
					continue;
				}
				if (!entry.getName().endsWith(".class")) {
					continue;
				}
				ClassParser classParser = isJar ? new ClassParser(zipFile, entry.getName()) : null;
				if (classParser == null) {
					System.err.println("JavaClassGenerator.getJavaClassesFromResource() - No class parser");
					continue;
				}
				try {
					JavaClass jc = classParser.parse();
					javaClasses.put(jc.getClassName(), jc);
				} catch (ClassFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			// Assume it's a directory
			String[] extensions = { "class" };
			Iterator<File> classesIter = FileUtils.iterateFiles(new File(resource), extensions, true);
			@SuppressWarnings("unchecked")
			Collection<File> files = IteratorUtils.toList(classesIter);
			for (File aClass : files) {
				try {
					ClassParser classParser = new ClassParser(checkNotNull(aClass.getAbsolutePath()));
					JavaClass jc = checkNotNull(checkNotNull(classParser).parse());
					javaClasses.put(jc.getClassName(), jc);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return javaClasses;
	}

	private static void visitJavaClass(JavaClass javaClass, RelationshipsV2 relationships) {
		try {
			new MyClassVisitorV2(javaClass, relationships).visitJavaClass(javaClass);
		} catch (ClassFormatException e) {
			e.printStackTrace();
		}
	}

	public void addMethodCall(String parentMethodQualifiedName, MyInstruction childMethod,
			String childMethodQualifiedName) {
		if ("java.lang.System.currentTimeMillis()".equals(parentMethodQualifiedName)) {
			throw new IllegalAccessError("No such thing");
		}
		if ("java.lang.System.currentTimeMillis()".equals(childMethodQualifiedName)) {
			// throw new IllegalAccessError("No such thing");
		}
		allMethodNameToMyInstructionMap.put(childMethodQualifiedName, childMethod);
		if (!parentMethodQualifiedName.equals(childMethodQualifiedName)) {// don't allow cycles
			if (parentMethodQualifiedName.contains("Millis")) {
				System.out.println("");
			}
			callingMethodToMethodInvocationMultiMap.put(parentMethodQualifiedName, childMethod);
		}
		if (!this.isVisitedMethod(childMethodQualifiedName)) {
			this.addUnvisitedMethod(childMethodQualifiedName);
		}
	}

	public boolean methodCallExists(String parentMethodQualifiedName, String childMethodQualifiedName) {
		for (MyInstruction childMethod : callingMethodToMethodInvocationMultiMap.get(parentMethodQualifiedName)) {
			if (childMethod.getMethodNameQualified().equals(childMethodQualifiedName)) {
				return true;
			}
		}
		return false;
	}

	private void addUnvisitedMethod(String childMethodQualifiedName) {
		this.isMethodVisited.put(childMethodQualifiedName, false);
	}

	@VisibleForTesting
	boolean isVisitedMethod(String childMethodQualifiedName) {
		if (!isMethodVisited.keySet().contains(childMethodQualifiedName)) {
			addUnvisitedMethod(childMethodQualifiedName);
		}
		return isMethodVisited.get(childMethodQualifiedName);
	}

	public void addContainmentRelationship(String parentClassFullName, JavaClass javaClass) {
		if (!Ignorer.shouldIgnore(javaClass)) {
			// System.err.println("CONTAINMENT: " + parentClassFullName + "--> " +
			// javaClass.getClassName());
		}
		classNameToFieldTypesMultiMap.put(parentClassFullName, javaClass);
		addContainmentRelationshipStringOnly(parentClassFullName, javaClass.getClassName());
	}

	public void addContainmentRelationshipStringOnly(String parentClassName, String childClassName) {
		if (parentClassName.equals("java.lang.Object")) {
			throw new IllegalAccessError("addContainmentRelationshipStringOnly");
		}
		if (childClassName.equals("java.lang.Object")) {
			throw new IllegalAccessError("addContainmentRelationshipStringOnly");
		}

		classNameToFieldTypeNamesMultiMap.put(parentClassName, childClassName);
		this.classNames.add(parentClassName);
		this.classNames.add(childClassName);
	}

	public Collection<String> getAllClassNames() {
		return ImmutableSet.copyOf(classNames);// classNameToJavaClassMap.keySet();
	}

	public Collection<String> getAllMethodCallers() {
		return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.keySet());
	}

	public Map<String, Collection<MyInstruction>> getAllMethodCallersMap() {
		return Multimaps.asMap(callingMethodToMethodInvocationMultiMap);
	}

	public Collection<MyInstruction> getCalledMethods(String parentMethodNameKey) {
		return ImmutableSet.copyOf(callingMethodToMethodInvocationMultiMap.get(parentMethodNameKey));
	}

	public Collection<JavaClass> getContainedClasses(String parentClassNameKey) {
		return ImmutableSet.copyOf(classNameToFieldTypesMultiMap.get(parentClassNameKey));
	}

	public Collection<String> getContainedClassNames(String parentClassNameKey) {
		return ImmutableSet.copyOf(classNameToFieldTypeNamesMultiMap.get(parentClassNameKey));
	}

	public int getMinPackageDepth() {
		return minPackageDepth;
	}

	public void updateMinPackageDepth(JavaClass javaClass) {
		int packageDepth = getPackageDepth(javaClass.getClassName());
		if (packageDepth < minPackageDepth) {
			minPackageDepth = packageDepth;
		}
	}

	public static int getPackageDepth(String qualifiedClassName) {
		String packageName = ClassUtils.getPackageName(qualifiedClassName);
		int periodCount = StringUtils.countMatches(packageName, ".");
		int packageDepth = periodCount + 1;
		return packageDepth;

	}

	public void addPackageOf(JavaClass classToVisit) {
		String pkgFullName = classToVisit.getPackageName();
		String parentPktFullName = ClassUtils.getPackageName(pkgFullName);
		this.parentPackageNameToChildPackageNameMultiMap.put(parentPktFullName, pkgFullName);
	}

	public JavaClass getClassDef(String aClassFullName) {
		JavaClass jc = null;
		try {
			jc = Repository.lookupClass(aClassFullName);
		} catch (ClassNotFoundException e) {
			if (this.classNameToJavaClassMap.get(aClassFullName) != null) {
				System.err.println(
						"SRIDHAR Relationships.getClassDef() - We do need our own homemade repository. I don't know why becl Repository.lookupClass() can't find "
								+ aClassFullName);
			}
		}
		if (jc == null) {
			jc = this.classNameToJavaClassMap.get(aClassFullName);
		}
		return jc;
	}

	public Collection<JavaClass> getParentClassesAndInterfaces(JavaClass childClass) {
		Collection<JavaClass> superClassesAndInterfaces = new HashSet<JavaClass>();
		String[] interfaceNames = childClass.getInterfaceNames();
		for (String interfaceName : interfaceNames) {
			JavaClass anInterface = this.classNameToJavaClassMap.get(interfaceName);
			if (anInterface == null) {
				// Do it later
				System.err.println(
						"    SRIDHAR Relationships.getParentClassesAndInterfaces() - deferring finding parent classes/interfaces of "
								+ interfaceName);
				deferParentContainment(interfaceName, childClass);
			} else {
				superClassesAndInterfaces.add(anInterface);
			}
		}
		String superclassNames = childClass.getSuperclassName();
		if (!superclassNames.equals("java.lang.Object")) {
			JavaClass theSuperclass = this.classNameToJavaClassMap.get(superclassNames);
			if (theSuperclass == null) {
				// Do it later
				deferParentContainment(superclassNames, childClass);
			} else {
				superClassesAndInterfaces.add(theSuperclass);
			}
		}
		if (superClassesAndInterfaces.size() > 0) {
			// System.err.println("Has a parent (" + childClass.getClassName() + ")");
		}
		return ImmutableSet.copyOf(superClassesAndInterfaces);
	}

	public boolean deferContainmentVisit(JavaClass parentClassToVisit, String childClassQualifiedName) {
		return this.deferredChildContainments
				.add(new DeferredChildContainment(parentClassToVisit, childClassQualifiedName));
	}

	public Set<DeferredChildContainment> getDeferredChildContainment() {
		return ImmutableSet.copyOf(this.deferredChildContainments);
	}

	public void validate() {
		if (this.allMethodNameToMyInstructionMap.keySet()
				.contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
			throw new IllegalAccessError("No such thing");
		}
		if (this.callingMethodToMethodInvocationMultiMap.keySet()
				.contains("com.rohidekar.callgraph.GraphNodeInstruction.getMethodNameQualified()")) {
			throw new IllegalAccessError("No such thing");
		}

	}

	public void deferSuperMethodRelationshipCapture(DeferredSuperMethod deferredSuperMethod) {
		this.deferredSuperMethod.add(deferredSuperMethod);
	}

	public Set<DeferredSuperMethod> getDeferSuperMethodRelationships() {
		return ImmutableSet.copyOf(this.deferredSuperMethod);
	}

	public void deferParentContainment(String parentClassName, JavaClass javaClass) {
		// System.err.println("Deferring " + parentClassName + " --> " +
		// javaClass.getClassName());
		this.deferredParentContainments.add(new DeferredParentContainment(parentClassName, javaClass));
	}

	public Set<DeferredParentContainment> getDeferredParentContainments() {
		return ImmutableSet.copyOf(deferredParentContainments);
	}

	public Set<String> getAllMethodNames() {
		return ImmutableSet.copyOf(allMethodNameToMyInstructionMap.keySet());
	}
	public Map<String, MyInstruction> getAllMethodNamesToMyInstructions() {
		return ImmutableMap.copyOf(allMethodNameToMyInstructionMap);
	}

	private Map<String, Boolean> isMethodVisited = new HashMap<String, Boolean>();

	public void setVisitedMethod(String parentMethodQualifiedName) {
		if (this.isMethodVisited.keySet().contains(parentMethodQualifiedName)) {
			this.isMethodVisited.remove(parentMethodQualifiedName);
		}
		this.isMethodVisited.put(parentMethodQualifiedName, true);
	}

	public MyInstruction getMethod(String qualifiedMethodName) {
		return this.allMethodNameToMyInstructionMap.get(qualifiedMethodName);
	}

	public void addMethodDefinition(MyInstruction myInstructionImpl) {
		allMethodNameToMyInstructionMap.put(myInstructionImpl.getMethodNameQualified(), myInstructionImpl);
	}

	public Collection<String> getPackagesKeySet() {
		return ImmutableSet.copyOf(this.parentPackageNameToChildPackageNameMultiMap.keySet());
	}

	public Collection<String> getChildPackagesOf(String parentPackage) {
		return ImmutableSet.copyOf(this.parentPackageNameToChildPackageNameMultiMap.get(parentPackage));
	}
}

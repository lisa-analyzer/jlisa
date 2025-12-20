package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.annotations.AnnotationInfo;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central context for parsing operations that manages program state, variable
 * types, location managers, and exception handling during the front-end parsing
 * process. This class serves as a coordination point for various parsing
 * activities and maintains the necessary state information for code analysis.
 * <p>
 * The parser context provides:
 * </p>
 * <ul>
 * <li>Variable type tracking across different CFGs</li>
 * <li>Exception handling with configurable strategies</li>
 * <li>Location manager creation for both real and synthetic code locations</li>
 * <li>Program-wide state management during parsing</li>
 * </ul>
 */
public class ParserContext {

	/** The program being parsed and analyzed */
	private Program program;

	/** The API level for the parsing context */
	private int apiLevel;

	/** Map of synthetic code location managers indexed by file name */
	private Map<String, SyntheticCodeLocationManager> syntheticCodeLocationManagers = new HashMap<>();

	/**
	 * Map storing variable types for each CFG, organized as CFG -> (variable
	 * name -> type)
	 */
	Map<CFG, Map<VariableInfo, Type>> variableTypes = new HashMap<>();

	// Method -> annotations found on that method (e.g., @GetMapping)
	private final Map<CodeMemberDescriptor, List<AnnotationInfo>> methodAnnotations = new HashMap<>();
	// Class -> annotations found on that class (e.g. @RestController)
	private final Map<JavaClassType, List<AnnotationInfo>> classAnnotations = new HashMap<>();
	// Field -> annotations found on that field (e.g. @Autowired on 'service')
	private final Map<String, List<AnnotationInfo>> fieldAnnotations = new HashMap<>();

	/**
	 * Constructs a new ParserContext with the specified program, API level, and
	 * exception handling strategy.
	 *
	 * @param program  the program to be parsed and analyzed
	 * @param apiLevel the API level for this parsing context exceptions
	 */
	public ParserContext(
			Program program,
			int apiLevel) {
		this.program = program;
		this.apiLevel = apiLevel;
	}

	/**
	 * Adds a variable type mapping for a specific CFG. This method tracks the
	 * static type of variables within the scope of a particular control flow
	 * graph.
	 *
	 * @param cfg          the control flow graph containing the variable
	 * @param type         the static type of the variable
	 * @param variableInfo the variable info
	 * 
	 * @throws RuntimeException if a variable with the same name already exists
	 *                              in the CFG
	 */
	public void addVariableType(
			CFG cfg,
			VariableInfo variableInfo,
			Type type) {
		Map<VariableInfo, Type> map = variableTypes.computeIfAbsent(cfg, k -> new HashMap<>());
		map.put(variableInfo, type);
	}

	/**
	 * Retrieves the static type of a variable by searching through the CFG's
	 * local variables, global variables in the containing unit and its
	 * ancestors, and finally checking if the name corresponds to a compilation
	 * unit.
	 * <p>
	 * The search order is:
	 * </p>
	 * <ol>
	 * <li>Local variables in the specified CFG</li>
	 * <li>Global variables in the containing compilation unit</li>
	 * <li>Instance globals in the containing compilation unit</li>
	 * <li>Variables in ancestor compilation units</li>
	 * <li>Check if the name is a compilation unit (returns JavaClassType)</li>
	 * <li>Return Untyped.INSTANCE if no match is found</li>
	 * </ol>
	 *
	 * @param cfg          the control flow graph to search within
	 * @param variableInfo the variable to look up
	 * 
	 * @return the static type of the variable, or Untyped.INSTANCE if not found
	 */
	public Type getVariableStaticType(
			CFG cfg,
			VariableInfo variableInfo) {

		Type type = null;

		Map<VariableInfo, Type> cfgVariables = variableTypes.get(cfg);
		if (cfgVariables != null) {
			type = cfgVariables.get(variableInfo);
			if (type == null) {
				type = cfgVariables.get(new VariableInfo(variableInfo.getName(), null));
			}
		}

		if (type == null) {
			String name = variableInfo.getName();
			type = getVariableStaticTypeFromUnitAndGlobals(cfg, name);
		}

		return type;
	}

	public Type getVariableStaticTypeFromUnitAndGlobals(
			CFG cfg,
			String name) {

		Unit unit = cfg.getDescriptor().getUnit();
		while (unit != null) {
			if (unit instanceof CompilationUnit) {
				CompilationUnit cu = (CompilationUnit) unit;
				for (Global g : cu.getGlobals()) {
					if (g.getName().equals(name)) {
						return g.getStaticType();
					}
				}
				for (Global g : cu.getInstanceGlobals(false)) {
					if (g.getName().equals(name)) {
						return g.getStaticType();
					}
				}
				if (cu.getImmediateAncestors().isEmpty()) {
					unit = null;
				} else {
					unit = cu.getImmediateAncestors().iterator().next();
				}
			} else {
				for (Global g : unit.getGlobals()) {
					if (g.getName().equals(name)) {
						return g.getStaticType();
					}
				}
			}

		}

		Unit u = program.getUnit(name);
		if (u instanceof CompilationUnit) {
			return JavaClassType.lookup(name);
		}
		return Untyped.INSTANCE;
	}

	/**
	 * Returns the program associated with this parser context.
	 *
	 * @return the program being parsed
	 */
	public Program getProgram() {
		return program;
	}

	/**
	 * Returns the API level for this parser context.
	 *
	 * @return the API level
	 */
	public int getApiLevel() {
		return apiLevel;
	}

	public void addMethodAnnotation(
			CodeMemberDescriptor member,
			AnnotationInfo ann) {
		methodAnnotations.computeIfAbsent(member, k -> new java.util.ArrayList<>()).add(ann);
	}

	public void addClassAnnotation(
			JavaClassType clazz,
			AnnotationInfo ann) {
		classAnnotations
				.computeIfAbsent(clazz, k -> new ArrayList<>())
				.add(ann);
	}

	public Map<CodeMemberDescriptor, List<AnnotationInfo>> getMethodAnnotations() {
		return Collections.unmodifiableMap(methodAnnotations);
	}

	public Map<JavaClassType, List<AnnotationInfo>> getClassAnnotations() {
		return Collections.unmodifiableMap(classAnnotations);
	}

	public List<AnnotationInfo> getClassAnnotations(
			JavaClassType clazz) {
		return classAnnotations.getOrDefault(clazz, Collections.emptyList());
	}

	public List<AnnotationInfo> getMethodAnnotations(
			CodeMemberDescriptor member) {
		List<AnnotationInfo> anns = methodAnnotations.get(member);
		return anns == null ? Collections.emptyList() : anns;
	}

	public void addFieldAnnotation(
			String fieldKey,
			AnnotationInfo ann) {
		fieldAnnotations
				.computeIfAbsent(fieldKey, k -> new ArrayList<>())
				.add(ann);
	}

	public Map<String, List<AnnotationInfo>> getFieldAnnotations() {
		return Collections.unmodifiableMap(fieldAnnotations);
	}

	public List<AnnotationInfo> getFieldAnnotations(
			String fieldKey) {
		return fieldAnnotations.getOrDefault(fieldKey, Collections.emptyList());
	}

	/**
	 * Creates and returns a new SourceCodeLocationManager for the specified
	 * file position. This manager is used for tracking real source code
	 * locations.
	 *
	 * @param fileName     the name of the source file
	 * @param lineNumber   the starting line number
	 * @param columnNumber the starting column number
	 * 
	 * @return a new SourceCodeLocationManager instance
	 */
	public SourceCodeLocationManager getLocationManager(
			String fileName,
			int lineNumber,
			int columnNumber) {
		return new SourceCodeLocationManager(fileName, lineNumber, columnNumber);
	}

	/**
	 * Retrieves or creates a SyntheticCodeLocationManager for the specified
	 * file. This manager is used for tracking synthetic (compiler-generated)
	 * code locations. The same manager instance is returned for subsequent
	 * calls with the same fileName. Using the manager provided by the parser
	 * ensures that the dispatched synthetic location is free.
	 * 
	 * @param fileName the name of the file to associate with synthetic
	 *                     locations
	 * 
	 * @return the SyntheticCodeLocationManager for the specified file
	 */
	public SyntheticCodeLocationManager getCurrentSyntheticCodeLocationManager(
			String fileName) {
		return syntheticCodeLocationManagers.computeIfAbsent(fileName, SyntheticCodeLocationManager::new);
	}

	public Global getGlobal(
			Unit unit,
			String targetName,
			boolean allowInstance) {
		Global global = null;

		if (allowInstance && unit instanceof CompilationUnit) {
			global = ((CompilationUnit) unit).getInstanceGlobal(targetName, true);
			if (global != null)
				return global;
		}

		global = unit.getGlobal(targetName);
		if (global != null)
			return global;

		if (unit instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) unit;
			for (Unit ancestor : cu.getImmediateAncestors()) {
				global = getGlobal(ancestor, targetName, allowInstance);
				if (global != null)
					return global;
			}
		}

		return null;
	}
}
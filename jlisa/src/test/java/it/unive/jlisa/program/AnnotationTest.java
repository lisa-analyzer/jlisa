package it.unive.jlisa.program;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;
import it.unive.lisa.program.annotations.values.BoolAnnotationValue;
import it.unive.lisa.program.annotations.values.CharAnnotationValue;
import it.unive.lisa.program.annotations.values.CompilationUnitAnnotationValue;
import it.unive.lisa.program.annotations.values.DoubleAnnotationValue;
import it.unive.lisa.program.annotations.values.FloatAnnotationValue;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.annotations.values.LongAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import it.unive.lisa.program.cfg.Parameter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AnnotationTest {

	private static Program lisaProgram;

	@BeforeAll
	static void parse() throws IOException {
		lisaProgram = new JavaFrontend().parseFromListOfFile(List.of(
				// Class-level
				"java-testcases/annotation/A.java",
				"java-testcases/annotation/B.java",
				"java-testcases/annotation/C.java",
				"java-testcases/annotation/D.java",

				// Method-level
				"java-testcases/annotation/E.java",
				"java-testcases/annotation/F.java",
				"java-testcases/annotation/G.java",

				// Constructor-level
				"java-testcases/annotation/H.java",
				"java-testcases/annotation/I.java",

				// Parameter-level in methods
				"java-testcases/annotation/J.java",
				"java-testcases/annotation/K.java",
				"java-testcases/annotation/L.java",
				"java-testcases/annotation/M.java",
				"java-testcases/annotation/N.java",

				// Special cases
				"java-testcases/annotation/S1.java",

				"java-testcases/annotation/Main.java"));
	}

	private static Collection<Annotation> getClassLevelAnnotations(
			String classUnitName) {
		return ((CompilationUnit) lisaProgram.getUnit(classUnitName)).getAnnotationList();
	}

	private static Collection<Annotation> getMethodLevelAnnotations(
			String classUnitName,
			String methodUnitName) {
		return ((CompilationUnit) lisaProgram.getUnit(classUnitName))
				.getInstanceCodeMembersByName(methodUnitName, false)
				.iterator().next()
				.getDescriptor().getAnnotationList();
	}

	private static Collection<Annotation> getParameterLevelAnnotations(
			String classUnitName,
			String codeMemberName,
			String parameterName) {
		Parameter[] params = ((CompilationUnit) lisaProgram.getUnit(classUnitName))
				.getInstanceCodeMembersByName(codeMemberName, false)
				.iterator().next()
				.getDescriptor().getFormals();

		for (Parameter param : params)
			if (param.getName().equals(parameterName))
				return param.getAnnotationList();

		throw new AssertionError("no parameter named " + parameterName + " in " + classUnitName + "." + codeMemberName);
	}

	private static void assertAnnotationOfClass(
			String classUnitName,
			Annotation isAnnotatedWith) {
		Collection<Annotation> anns = getClassLevelAnnotations(classUnitName);

		assertTrue(anns.contains(isAnnotatedWith),
				"unit " + classUnitName + " should carry " + isAnnotatedWith + ", got " + anns);
	}

	private static void assertAnnotationOfMethod(
			String classUnitName,
			String methodUnitName,
			Annotation isAnnotatedWith) {
		Collection<Annotation> anns = getMethodLevelAnnotations(classUnitName, methodUnitName);

		assertTrue(anns.contains(isAnnotatedWith),
				classUnitName + "." + methodUnitName + " should carry " + isAnnotatedWith + ", got " + anns);
	}

	private static void assertAnnotationOfParameter(
			String classUnitName,
			String methodUnitName,
			String parameterName,
			Annotation isAnnotatedWith) {
		Collection<Annotation> anns = getParameterLevelAnnotations(classUnitName, methodUnitName, parameterName);

		assertTrue(anns.contains(isAnnotatedWith),
				classUnitName + "." + methodUnitName + "(" + parameterName + ") should carry " + isAnnotatedWith
						+ ", got " + anns);
	}

	private static void assertParameterHasNoAnnotations(
			String classUnitName,
			String methodUnitName,
			String parameterName) {
		Collection<Annotation> anns = getParameterLevelAnnotations(classUnitName, methodUnitName, parameterName);

		assertTrue(anns.isEmpty(),
				classUnitName + "." + methodUnitName + "(" + parameterName + ") should carry no annotations, got "
						+ anns);
	}

	@Test
	public void testClassLevelMarkerAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ClassLevelMarkerTypeAnnotation");

		assertAnnotationOfClass("A", isAnnotatedWith);
	}

	@Test
	public void testClassLevelSingleMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ClassLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new IntAnnotationValue(22))));

		assertAnnotationOfClass("B", isAnnotatedWith);
	}

	@Test
	public void testNormalAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ClassLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(1)),
				new AnnotationMember("value2", new StringAnnotationValue("\"a\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(true))));

		assertAnnotationOfClass("C", isAnnotatedWith);
	}

	@Test
	public void testNormalAnnotationWithArray() {
		Annotation isAnnotatedWith = new Annotation("ClassLevelAnnotationInherited", List.of(
				new AnnotationMember("value1", new FloatAnnotationValue(1.5f)),
				new AnnotationMember("value2", new DoubleAnnotationValue(2.5)),
				new AnnotationMember("value3", new LongAnnotationValue(99L)),
				new AnnotationMember("value4", new IntAnnotationValue(7)),
				new AnnotationMember("value5", new ArrayAnnotationValue(new BasicAnnotationValue[] {
						new StringAnnotationValue("\"x\""),
						new StringAnnotationValue("\"y\"") }))));

		assertAnnotationOfClass("D", isAnnotatedWith);
	}

	@Test
	public void testMethodLevelMarkerAnnotation() {
		Annotation isAnnotatedWith = new Annotation("MethodLevelMarkerTypeAnnotation");

		assertAnnotationOfMethod("E", "E_1", isAnnotatedWith);
	}

	@Test
	public void testMethodLevelSingleMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("MethodLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new StringAnnotationValue("\"a\""))));

		assertAnnotationOfMethod("F", "F_2", isAnnotatedWith);
	}

	@Test
	public void testMethodLevelNormalAnnotation() {
		Annotation isAnnotatedWith = new Annotation("MethodLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new CharAnnotationValue('a')),
				new AnnotationMember("value2", new IntAnnotationValue(1)),
				new AnnotationMember("value3", new BoolAnnotationValue(false))));

		assertAnnotationOfMethod("G", "G_1", isAnnotatedWith);
	}

	@Test
	public void testConstructorLevelMarkerAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ConstructorLevelMarkerTypeAnnotation");

		assertAnnotationOfMethod("H", "H", isAnnotatedWith);
	}

	@Test
	public void testConstructorLevelNormalAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ConstructorLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new StringAnnotationValue("\"a\"")),
				new AnnotationMember("value2", new LongAnnotationValue(99L))));

		assertAnnotationOfMethod("I", "I", isAnnotatedWith);
	}

	@Test
	public void testParameterLevelSingleMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ParameterLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new StringAnnotationValue("\"paramVal\""))));

		assertAnnotationOfParameter("J", "J_1", "param1", isAnnotatedWith);
	}

	@Test
	public void testParameterLevelMarkerAnnotation() {
		Annotation isAnnotatedWith = new Annotation("ParameterLevelMarkerTypeAnnotation");

		assertAnnotationOfParameter("K", "K_1", "param1", isAnnotatedWith);
		assertAnnotationOfParameter("K", "K_1", "param3", isAnnotatedWith);
		assertParameterHasNoAnnotations("K", "K_1", "param2");
		assertParameterHasNoAnnotations("K", "K_2", "param");
	}

	@Test
	public void testParameterLevelNormalAnnotation() {
		Annotation onParam1 = new Annotation("ParameterLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(1)),
				new AnnotationMember("value2", new StringAnnotationValue("\"paramVal\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(false)),
				new AnnotationMember("value4", new CharAnnotationValue('p'))));
		Annotation onParam2 = new Annotation("ParameterLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(2)),
				new AnnotationMember("value2", new StringAnnotationValue("\"paramVal2\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(true)),
				new AnnotationMember("value4", new CharAnnotationValue('a'))));

		assertAnnotationOfParameter("L", "L_2", "param1", onParam1);
		assertAnnotationOfParameter("L", "L_2", "param2", onParam2);
		assertParameterHasNoAnnotations("L", "L_2", "param3");
		assertParameterHasNoAnnotations("L", "L_1", "param");
	}

	@Test
	public void testParameterLevelVarargsAnnotation() {
		Annotation single = new Annotation("ParameterLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new StringAnnotationValue("\"paramVal\""))));
		Annotation varargs = new Annotation("ParameterLevelVarargsTypeAnnotation");

		assertAnnotationOfParameter("M", "M_1", "param1", single);
		assertAnnotationOfParameter("M", "M_1", "param2", varargs);
	}

	@Test
	public void testConstructorParameterLevelAnnotations() {
		Annotation single = new Annotation("ParameterLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new StringAnnotationValue("\"paramVal\""))));
		Annotation normal = new Annotation("ParameterLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(10)),
				new AnnotationMember("value2", new StringAnnotationValue("\"b\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(false)),
				new AnnotationMember("value4", new CharAnnotationValue('s'))));

		assertAnnotationOfParameter("N", "N", "param1", single);
		assertAnnotationOfParameter("N", "N", "param2", normal);
		assertParameterHasNoAnnotations("N", "N", "param3");
	}

	@Test
	public void testClassLevelTypeLiteralMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("AnnotationWithTypeLiteralMember",
				List.of(new AnnotationMember("value", new CompilationUnitAnnotationValue("A"))));

		assertAnnotationOfClass("S1", isAnnotatedWith);
	}
}

package it.unive.jlisa.program;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;
import it.unive.lisa.program.annotations.values.BoolAnnotationValue;
import it.unive.lisa.program.annotations.values.CharAnnotationValue;
import it.unive.lisa.program.annotations.values.CompilationUnitAnnotationValue;
import it.unive.lisa.program.annotations.values.DoubleAnnotationValue;
import it.unive.lisa.program.annotations.values.EnumAnnotationValue;
import it.unive.lisa.program.annotations.values.FloatAnnotationValue;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.annotations.values.LongAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import it.unive.lisa.program.cfg.Parameter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AnnotationTest {

	private static Program lisaProgram;

	private static final List<String> capturedLoggerWarnings = new CopyOnWriteArrayList<>();
	private static Logger annotationBuilderLogger;
	private static AbstractAppender captureAppender;

	private static final class CapturingAppender extends AbstractAppender {
		CapturingAppender() {
			super("AnnotationTestCapture", null, null, false, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(
				LogEvent event) {
			if (event.getLevel().isMoreSpecificThan(Level.WARN))
				capturedLoggerWarnings.add(event.getMessage().getFormattedMessage());
		}
	}

	@BeforeAll
	static void parse() throws IOException {
		captureAppender = new CapturingAppender();
		captureAppender.start();
		annotationBuilderLogger = (Logger) LogManager.getLogger("it.unive.jlisa.frontend.util.AnnotationBuilder");
		annotationBuilderLogger.addAppender(captureAppender);

		lisaProgram = new JavaFrontend().parseFromListOfFile(List.of(

				// Load definitions first
				"java-testcases/annotation/special-case/Constants.java",
				"java-testcases/annotation/special-case/Enumeration.java",

//				// Class-level
				"java-testcases/annotation/A.java",
				"java-testcases/annotation/B.java",
				"java-testcases/annotation/C.java",
				"java-testcases/annotation/D.java",

//				// Method-level
				"java-testcases/annotation/E.java",
				"java-testcases/annotation/F.java",
				"java-testcases/annotation/G.java",

//				// Constructor-level
				"java-testcases/annotation/H.java",
				"java-testcases/annotation/I.java",

//				// Parameter-level in methods
				"java-testcases/annotation/J.java",
				"java-testcases/annotation/K.java",
				"java-testcases/annotation/L.java",
				"java-testcases/annotation/M.java",
				"java-testcases/annotation/N.java",

//				// Field-level
				"java-testcases/annotation/O.java",
				"java-testcases/annotation/P.java",
				"java-testcases/annotation/Q.java",

				// Special cases
				"java-testcases/annotation/S1.java",
				"java-testcases/annotation/S2.java",
				"java-testcases/annotation/S3.java",
				"java-testcases/annotation/S4.java",

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

	private static Collection<Annotation> getFieldLevelAnnotations(
			String classUnitName,
			String fieldName) {
		CompilationUnit unit = (CompilationUnit) lisaProgram.getUnit(classUnitName);

		Global field = unit.getInstanceGlobal(fieldName, false);
		if (field == null)
			field = unit.getGlobal(fieldName);

		if (field == null)
			throw new AssertionError("no field named " + fieldName + " in " + classUnitName);

		return field.getAnnotationList();
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

	private static void assertAnnotationOfField(
			String classUnitName,
			String fieldName,
			Annotation isAnnotatedWith) {
		Collection<Annotation> anns = getFieldLevelAnnotations(classUnitName, fieldName);

		assertTrue(anns.contains(isAnnotatedWith),
				classUnitName + "#" + fieldName + " should carry " + isAnnotatedWith + ", got " + anns);
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
		Annotation isAnnotatedWith1 = new Annotation("ParameterLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(1)),
				new AnnotationMember("value2", new StringAnnotationValue("\"paramVal\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(false)),
				new AnnotationMember("value4", new CharAnnotationValue('p'))));
		Annotation isAnnotatedWith2 = new Annotation("ParameterLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new IntAnnotationValue(2)),
				new AnnotationMember("value2", new StringAnnotationValue("\"paramVal2\"")),
				new AnnotationMember("value3", new BoolAnnotationValue(true)),
				new AnnotationMember("value4", new CharAnnotationValue('a'))));

		assertAnnotationOfParameter("L", "L_2", "param1", isAnnotatedWith1);
		assertAnnotationOfParameter("L", "L_2", "param2", isAnnotatedWith2);
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
	public void testFieldLevelMarkerAnnotation() {
		Annotation isAnnotatedWith = new Annotation("FieldLevelMarkerTypeAnnotation");

		assertAnnotationOfField("O", "field1", isAnnotatedWith);
		assertAnnotationOfField("O", "field2", isAnnotatedWith);
		assertAnnotationOfField("O", "field3", isAnnotatedWith);
		assertAnnotationOfField("O", "field4", isAnnotatedWith);
		assertAnnotationOfField("O", "field5", isAnnotatedWith);
		assertAnnotationOfField("O", "field6", isAnnotatedWith);
	}

	@Test
	public void testFieldLevelSingleMemberAnnotation() {
		Annotation isAnnotatedWith1 = new Annotation("FieldLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new IntAnnotationValue(1))));
		Annotation isAnnotatedWith2 = new Annotation("FieldLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new IntAnnotationValue(2))));
		Annotation isAnnotatedWith3 = new Annotation("FieldLevelSingleMemberAnnotation",
				List.of(new AnnotationMember("value", new IntAnnotationValue(3))));

		assertAnnotationOfField("P", "field1", isAnnotatedWith1);
		assertAnnotationOfField("P", "field2", isAnnotatedWith2);
		assertAnnotationOfField("P", "field3", isAnnotatedWith3);
	}

	@Test
	public void testFieldLevelNormalAnnotation() {
		Annotation isAnnotatedWith1 = new Annotation("FieldLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new LongAnnotationValue(1000L)),
				new AnnotationMember("value2", new StringAnnotationValue("\"abc\"")),
				new AnnotationMember("value3", new CharAnnotationValue('x'))));
		Annotation isAnnotatedWith2 = new Annotation("FieldLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new LongAnnotationValue(2000L)),
				new AnnotationMember("value2", new StringAnnotationValue("\"dfg\"")),
				new AnnotationMember("value3", new CharAnnotationValue('y'))));
		Annotation isAnnotatedWith3 = new Annotation("FieldLevelNormalAnnotation", List.of(
				new AnnotationMember("value", new LongAnnotationValue(3000L)),
				new AnnotationMember("value2", new StringAnnotationValue("\"hji\"")),
				new AnnotationMember("value3", new CharAnnotationValue('z'))));

		assertAnnotationOfField("Q", "field1", isAnnotatedWith1);
		assertAnnotationOfField("Q", "field2", isAnnotatedWith2);
		assertAnnotationOfField("Q", "field3", isAnnotatedWith3);
	}

	@Test
	public void testClassLevelTypeLiteralMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("AnnotationWithTypeLiteralMember",
				List.of(new AnnotationMember("value", new CompilationUnitAnnotationValue("A"))));

		assertAnnotationOfClass("S1", isAnnotatedWith);
	}

	@Test
	public void testQualifiedNameMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("AnnotationWithQualifiedNameMember", List.of(
				new AnnotationMember("value", new StringAnnotationValue("\"string-constant-value\"")),
				new AnnotationMember("value2", new IntAnnotationValue(100)),
				new AnnotationMember("value3", new BoolAnnotationValue(true))));

		assertAnnotationOfMethod("S2", "s2", isAnnotatedWith);
	}

	@Test
	public void testUnresolvableQualifiedNameMember() {
		Annotation isAnnotatedWith = new Annotation("AnnotationWithQualifiedNameMember", List.of(
				new AnnotationMember("value", new StringAnnotationValue("Constants.UNDEFINED_CONSTANT"))));

		assertAnnotationOfMethod("S3", "s3", isAnnotatedWith);
		assertTrue(
				capturedLoggerWarnings.stream().anyMatch(m -> m.contains("Constants.UNDEFINED_CONSTANT")),
				"expected a WARN about the unresolved QualifiedName, captured: " + capturedLoggerWarnings);
	}

	@Test
	public void testEnumAsQualifiedNameMemberAnnotation() {
		Annotation isAnnotatedWith = new Annotation("AnnotationWithEnumAsQualifiedNameMember", List.of(
				new AnnotationMember("value", new EnumAnnotationValue("Enumeration", "Enum1")),
				new AnnotationMember("value2", new EnumAnnotationValue("Enumeration", "Enum3"))));

		assertAnnotationOfMethod("S4", "s4", isAnnotatedWith);
	}

	@AfterAll
	static void tearDown() {
		if (annotationBuilderLogger != null && captureAppender != null)
			annotationBuilderLogger.removeAppender(captureAppender);
		if (captureAppender != null)
			captureAppender.stop();
	}
}

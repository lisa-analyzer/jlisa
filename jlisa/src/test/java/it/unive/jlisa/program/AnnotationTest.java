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
import it.unive.lisa.program.annotations.values.DoubleAnnotationValue;
import it.unive.lisa.program.annotations.values.FloatAnnotationValue;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.annotations.values.LongAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;

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
                "java-testcases/annotation/A.java",
                "java-testcases/annotation/B.java",
                "java-testcases/annotation/C.java",
                "java-testcases/annotation/D.java",
                "java-testcases/annotation/E.java",
                "java-testcases/annotation/F.java",
                "java-testcases/annotation/G.java",
                "java-testcases/annotation/H.java",
                "java-testcases/annotation/I.java",
                "java-testcases/annotation/Main.java"));
    }

    private static Collection<Annotation> getClassLevelAnnotations(String classUnitName) {
        return ((CompilationUnit) lisaProgram.getUnit(classUnitName)).getAnnotations().getAnnotations();
    }

    private static Collection<Annotation> getMethodLevelAnnotations(String classUnitName, String methodUnitName ) {
        return ((CompilationUnit) lisaProgram.getUnit(classUnitName))
                .getInstanceCodeMembersByName(methodUnitName, false)
                .iterator().next()
                .getDescriptor().getAnnotations().getAnnotations();
    }

    private static void assertAnnotationOfClass(String classUnitName, Annotation isAnnotatedWith) {
        Collection<Annotation> anns = getClassLevelAnnotations(classUnitName);

        assertTrue(anns.contains(isAnnotatedWith), "unit " + classUnitName + " should carry " + isAnnotatedWith + ", got " + anns);
    }

    private static void assertAnnotationOfMethod(String classUnitName, String methodUnitName, Annotation isAnnotatedWith) {
        Collection<Annotation> anns = getMethodLevelAnnotations(classUnitName, methodUnitName);

        assertTrue(anns.contains(isAnnotatedWith),
                classUnitName + "." + methodUnitName + " should carry " + isAnnotatedWith + ", got " + anns);
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
                new AnnotationMember("value5", new ArrayAnnotationValue(new BasicAnnotationValue[]{
                        new StringAnnotationValue("\"x\""),
                        new StringAnnotationValue("\"y\"")}))));

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
}

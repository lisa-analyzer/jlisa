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

    private static Program program;

    @BeforeAll
    static void parse() throws IOException {
        program = new JavaFrontend().parseFromListOfFile(List.of(
                "java-testcases/annotation/A.java",
                "java-testcases/annotation/B.java",
                "java-testcases/annotation/C.java",
                "java-testcases/annotation/D.java",
                "java-testcases/annotation/Main.java"));
    }

    private static void assertAnnotation(String unitName, Annotation expected) {
        Collection<Annotation> anns = ((CompilationUnit) program.getUnit(unitName)).getAnnotations().getAnnotations();
        assertTrue(anns.contains(expected), "unit " + unitName + " should carry " + expected + ", got " + anns);
    }

    @Test
    public void testMarkerAnnotation() {
        Annotation expected = new Annotation("ClassLevelMarkerTypeAnnotation");
        assertAnnotation("A", expected);
    }

    @Test
    public void testSingleMemberAnnotation() {
        Annotation expected = new Annotation("ClassLevelSingleMemberAnnotation",
                List.of(new AnnotationMember("value", new IntAnnotationValue(22))));

        assertAnnotation("B", expected);
    }

    @Test
    public void testNormalAnnotation() {
        Annotation expected = new Annotation("ClassLevelNormalAnnotation", List.of(
                new AnnotationMember("value", new IntAnnotationValue(1)),
                new AnnotationMember("value2", new StringAnnotationValue("\"a\"")),
                new AnnotationMember("value3", new BoolAnnotationValue(true))));

        assertAnnotation("C", expected);
    }

    @Test
    public void testNormalAnnotationWithArray() {
        Annotation expected = new Annotation("ClassLevelAnnotationInherited", List.of(
                new AnnotationMember("value1", new FloatAnnotationValue(1.5f)),
                new AnnotationMember("value2", new DoubleAnnotationValue(2.5)),
                new AnnotationMember("value3", new LongAnnotationValue(99L)),
                new AnnotationMember("value4", new IntAnnotationValue(7)),
                new AnnotationMember("value5", new ArrayAnnotationValue(new BasicAnnotationValue[]{
                        new StringAnnotationValue("\"x\""),
                        new StringAnnotationValue("\"y\"")}))));

        assertAnnotation("D", expected);
    }
}

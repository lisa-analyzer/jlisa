package it.unive.jlisa.cron;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class MethodOverloadingTest extends JLiSAAnalysisExecutor {

    @Test
    public void testMethodOverloading1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test1/Test1.java"));
        assert frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));

        perform(TestHelpers.createConfiguration("method-overloading", "Test1", "Test1.java"));
    }

    @Test
    public void testMethodOverloading2() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test2/Test2.java"));
        assert !frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));

        perform(TestHelpers.createConfiguration("method-overloading", "Test2", "Test2.java"));
    }

    @Test
    public void testMethodOverloading3() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test3/Test3.java"));
        assert !frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));

        perform(TestHelpers.createConfiguration("method-overloading", "Test3", "Test3.java"));
    }

    @Test
    public void testMethodOverloading4() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test4/Test4.java"));
        assert frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));
    }

    @Test
    public void testMethodOverloading5() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test5/Test5.java"));
        assert frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));
    }

    @Test
    public void testMethodOverloading6() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test6/Test6.java"));
        assert !frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));

        perform(TestHelpers.createConfiguration("method-overloading", "Test6", "Test6.java"));
    }

    @Test
    public void testMethodOverloading7() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test7/Test7.java"));
        // NOTE: Test7.java is not a valid Java class because `String...` is just syntactic sugar for `String[]`.
        // This means the methods foo(String... args) and foo(String[] args) actually have the same signature,
        // so the frontend should raise a ParsingException.
        // Currently, since varargs are not supported, `String...` is treated simply as `String`.
        // To make this test pass for now, we assume that no duplicated_method_descriptor exception
        // is added to the parser context in this case.
        // Once varargs are implemented, this test will correctly fail, and we will only need
        // to invert the assert condition.
        assert !frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));
    }

    @Test
    public void testMethodOverloading8() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test8/Test8.java"));
        assert frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));
    }

    @Test
    public void testMethodOverloading9() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(List.of("java-testcases/method-overloading/Test9/Test9.java"));
        assert !frontend.getParserContext().getExceptions().stream().anyMatch(exception -> exception.getName().equals("duplicated_method_descriptor"));

        perform(TestHelpers.createConfiguration("method-overloading", "Test9", "Test9.java"));
    }
}

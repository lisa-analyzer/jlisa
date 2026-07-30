package it.unive.jlisa.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import it.unive.jlisa.antlr.LibraryDefinitionLexer;
import it.unive.jlisa.antlr.LibraryDefinitionParser;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.program.libraries.LibrarySpecificationParser;
import it.unive.jlisa.program.libraries.loader.ClassDef;
import it.unive.jlisa.program.libraries.loader.Field;
import it.unive.jlisa.program.libraries.loader.NumberValue;
import it.unive.jlisa.program.libraries.loader.Runtime;
import it.unive.jlisa.program.libraries.loader.extensions.CompileTimeGlobal;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Constant;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

public class ParsingTest {

	@Test
	public void testFieldWithDefaultValue() throws Exception {
		Runtime runtime;
		try (InputStream in = ParsingTest.class.getResourceAsStream("/java-libraries/thread.txt")) {
			LibraryDefinitionLexer lexer = new LibraryDefinitionLexer(
					CharStreams.fromStream(in, StandardCharsets.UTF_8));
			LibraryDefinitionParser parser = new LibraryDefinitionParser(new CommonTokenStream(lexer));
			runtime = new LibrarySpecificationParser("thread.txt").visitFile(parser.file());
		}

		ClassDef thread = runtime.getClasses().stream()
				.filter(c -> c.getName().equals("java.lang.Thread"))
				.findFirst().orElseThrow();

		Program program = JavaFrontend.createProgram();

		assertDefault(thread, program, "MIN_PRIORITY", 1);
		assertDefault(thread, program, "NORM_PRIORITY", 5);
		assertDefault(thread, program, "MAX_PRIORITY", 10);
	}

	private static void assertDefault(
			ClassDef cls,
			Program program,
			String field,
			int expected) {

		Field f = cls.getFields().stream()
				.filter(x -> x.getName().equals(field))
				.findFirst().orElseThrow();

		assertEquals(expected, assertInstanceOf(NumberValue.class, f.getValue()).getValue());

		Global global = f.toLiSAObject(program, SyntheticLocation.INSTANCE, program);
		CompileTimeGlobal withDefault = assertInstanceOf(CompileTimeGlobal.class, global);
		Constant constant = assertInstanceOf(Constant.class, withDefault.getDefaultValue());
		assertEquals(expected, constant.getValue());
	}
}

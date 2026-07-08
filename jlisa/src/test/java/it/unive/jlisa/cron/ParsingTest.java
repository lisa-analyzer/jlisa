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
import it.unive.jlisa.program.libraries.loader.extensions.GlobalWithDefault;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
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
		CFG init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "param_init"));

		assertDefault(thread, program, init, "MIN_PRIORITY", 1);
		assertDefault(thread, program, init, "NORM_PRIORITY", 5);
		assertDefault(thread, program, init, "MAX_PRIORITY", 10);
	}

	private static void assertDefault(
			ClassDef cls,
			Program program,
			CFG init,
			String field,
			int expected) {

		Field f = cls.getFields().stream()
				.filter(x -> x.getName().equals(field))
				.findFirst().orElseThrow();

		assertEquals(expected, assertInstanceOf(NumberValue.class, f.getValue()).getValue());

		Global global = f.toLiSAObject(program, SyntheticLocation.INSTANCE, init, program);
		GlobalWithDefault withDefault = assertInstanceOf(GlobalWithDefault.class, global);
		Int32Literal literal = assertInstanceOf(Int32Literal.class, withDefault.getDefaultValue());
		assertEquals(expected, literal.getValue().intValue());
	}
}

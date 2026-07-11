package it.unive.jlisa.cron;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.outputs.HtmlResults;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ReflectionTest extends JLiSAAnalysisExecutor {
	@Test
	public void testClassForName1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-for-name-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassForName2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-for-name-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassNewInstance1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-new-instance-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassNewInstance2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-new-instance-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testGetField1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-field-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testGetField2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-field-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testFieldGetSet1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "field-get-set-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testFieldGet1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "field-get-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testFieldSetStatic1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "field-set-static-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassGetMethod1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-method-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassGetMethod2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-method-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassGetMethods1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "class-get-methods-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testMethodInvoke1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "method-invoke-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testMethodInvoke2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "method-invoke-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testClassForNameInterface() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection",
				"class-for-name-interface", "Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testFieldGetStatic1() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "field-get-static-1",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}

	@Test
	public void testFieldGetStatic2() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("reflection", "field-get-static-2",
				"Main.java");
		conf.outputs.add(new HtmlResults(true));
		perform(conf);
	}
}

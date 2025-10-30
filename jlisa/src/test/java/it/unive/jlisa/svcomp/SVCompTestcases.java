package it.unive.jlisa.svcomp;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class SVCompTestcases extends JLiSAAnalysisExecutor {

	@Test
	public void instanceof1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void instanceof3_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof3",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void instanceof4_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof4",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void instanceof5_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof5",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void iarith1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void iarith2_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "iarith2",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void return1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "return1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void virtual2_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "virtual2",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void athrow1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "athrow1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void boolean1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "boolean1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void const1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "const1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StaticCharMethods01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods01",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StaticCharMethods04_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StaticCharMethods04",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void interface1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "interface1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void NullPointerException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NullPointerException1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void NullPointerException4_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "NullPointerException4",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void ClassCastException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "ClassCastException1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void NegativeArraySizeException1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"NegativeArraySizeException1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void exceptions1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void exceptions6_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions6",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void exceptions8_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions8",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void exceptions16_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions16",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringValueOf05_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf05",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringContains02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringContains02",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void ArithmeticException5_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "ArithmeticException5",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void ArithmeticException6_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "ArithmeticException6",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void Class_method1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "Class_method1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void instanceof8_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "instanceof8",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringValueOf08_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf08",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringValueOf09_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf09",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringConcatenation01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringConcatenation01",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void exceptions9_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "exceptions9",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringValueOf02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf02",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void SubString02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "SubString02",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void SubString03_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "SubString03",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void CharSequenceBug_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "CharSequenceBug",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void StringCompare01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringCompare01",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Test
	public void if_icmp1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "if_icmp1",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Ignore
	public void urldecoder1_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "urldecoder01",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Ignore
	public void urldecoder2_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "urldecoder02",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Ignore
	public void Basic1_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "Basic1",
				"Main.java",
				"../common/",
				"../mock/",
				"../mockx/",
				"../securibench/");
		perform(conf);
	}

	@Test
	public void spec1_5_product1_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "spec1-5_product1",
				"Main.java",
				"Actions.java",
				"../common/",
				"MinePumpSystem/Environment.java",
				"MinePumpSystem/MinePump.java");
		perform(conf);
	}

	@Test
	public void spec1_5_product3_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "spec1-5_product3",
				"Main.java",
				"Actions.java",
				"../common/",
				"MinePumpSystem/Environment.java",
				"MinePumpSystem/MinePump.java");
		perform(conf);
	}

	@Test
	public void lock_00_01_10_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "lock-00-01-10",
				"Main.java",
				"../common/",
				"rtems/",
				"harness/",
				"base/");
		perform(conf);
	}

	@Ignore
	// this test sometimes causes oom during dumping when executing
	// from gradle
	public void siena_eqchk_prop1_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "siena-eqchk-1",
				"../common/",
				"impl/",
				"prop1/");
		perform(conf);
	}

	@Test
	public void Inheritance1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "inheritance1",
				"Main.java");
		perform(conf);
	}

	@Test
	public void BinaryTreeSearchMemSat01_test() throws IOException {
		// this test had troublesome parsing errors,
		// but it is too complex to thoroughly check its results
		// at this stage. we just check that it runs to completion
		// without inspecting the results (ie no json files are present)
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "BinaryTreeSearch-MemSat01",
				"Main.java", "../common/");
		perform(conf);
	}

	@Ignore
	public void SortedListInsert_FunUnsat01_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"SortedListInsert-FunUnsat01",
				"Main.java", "../common/");
		perform(conf);
	}

	@Test
	public void MathSin_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "MathSin",
				"Main.java", "../common/", "MathSin.java");
		perform(conf);
	}

	@Test
	public void VelocityTracker_true_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "VelocityTracker_true",
				"Main.java", "../common/");
		perform(conf);
	}

	@Test
	public void swap1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "swap1",
				"Main.java");
		perform(conf);
	}

	@Test
	public void StringConstructors01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringConstructors01",
				"Main.java");
		perform(conf);
	}

	@Ignore
	// FIXME there seem to be some nondeterminism in this test
	public void SortedListInsert_MemUnsat01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"SortedListInsert-MemUnsat01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void RegexSubstitution02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "RegexSubstitution02",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringValueOf01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringValueOf01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void radians_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "radians",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringMiscellaneous01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringMiscellaneous01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringStartEnd01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringStartEnd01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void TokenTest02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "TokenTest02",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderAppend01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringBuilderAppend01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderAppend02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "StringBuilderAppend02",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderInsertDelete01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderInsertDelete01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderInsertDelete02_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderInsertDelete02",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderInsertDelete03_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderInsertDelete03",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void ExSymExe_true_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"ExSymExe_true",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void RedBlackTree_FunUnsat01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"RedBlackTree-FunUnsat01",
				"Main.java", "rbtree/Node.java", "rbtree/RedBlackTree.java", "rbtree/RedBlackTreeNode.java",
				"../common");
		perform(conf);
	}

	@Test
	public void ExSymExeLongBytecodes_false_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"ExSymExeLongBytecodes_false",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderChars05_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderChars05",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderChars03_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderChars03",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void exceptions10_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"exceptions10",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void StaticCharMethods06_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StaticCharMethods06",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void RegexSubstitution01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"RegexSubstitution01",
				"Main.java", "../common");
		perform(conf);
	}

	@Ignore
	public void BellmanFord_MemUnsat01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"BellmanFord-MemUnsat01",
				"Main.java", "../common");
		perform(conf);
	}
}

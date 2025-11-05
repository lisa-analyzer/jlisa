package it.unive.jlisa.svcomp;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
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

	@Disabled // TODO this seems nodeterministic on vignole
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

	@Disabled
	public void urldecoder1_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "urldecoder01",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Disabled
	public void urldecoder2_test() throws IOException {
		// FIXME there seem to be some nondeterminism in this test
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "urldecoder02",
				"Main.java",
				"../common/");
		perform(conf);
	}

	@Disabled
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

	@Disabled
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

	@Disabled
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

	@Disabled
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

	@Ignore
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

	@Test
	public void BellmanFord_MemUnsat01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"BellmanFord-MemUnsat01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void constructor1_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"constructor1",
				"Main.java", "../common");
		perform(conf);
	}

	@Ignore
	// FIXME there seem to be some nondeterminism in this test
	public void objects01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"objects01",
				"Main.java", "../common", "../classes");
		perform(conf);
	}

	@Test
	public void CWE369_Divide_by_Zero__float_connect_tcp_divide_01_bad_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"CWE369_Divide_by_Zero__float_connect_tcp_divide_01_bad",
				"Main.java", "testcasesupport", "../common");
		perform(conf);
	}

	@Test
	public void StringBuilderCapLen01_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"StringBuilderCapLen01",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void FractalTouchHandler_false_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp", "FractalTouchHandler_false",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Math_public_static_long_java_lang_Math_abs_long() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Math_public_static_long_java_lang_Math_abs_long",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Math_public_static_int_java_lang_Math_round_float() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Math_public_static_int_java_lang_Math_round_float",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Math_public_static_long_java_lang_Math_max_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Math_public_static_long_java_lang_Math_max_long_long",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Long_public_static_int_java_lang_Long_bitCount_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_bitCount_long",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Long_public_static_long_java_lang_Long_rotateRight_long_int_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_rotateRight_long_int",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Long_public_static_int_java_lang_Long_compare_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_compare_long_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_int_java_lang_Long_compareUnsigned_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_compareUnsigned_long_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_long_java_lang_Long_longValue_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_long_java_lang_Long_longValue",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_int_java_lang_Long_compareTo_java_lang_Long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_int_java_lang_Long_compareTo_java_lang_Long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_Long_java_lang_Long_valueOf_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_Long_java_lang_Long_valueOf_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_float_java_lang_Long_floatValue_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_float_java_lang_Long_floatValue",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_reverseBytes_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_reverseBytes_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_Long_java_lang_Long_getLong_java_lang_String_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_Long_java_lang_Long_getLong_java_lang_String_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_sum_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_sum_long_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_min_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_min_long_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_highestOneBit_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_highestOneBit_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toBinaryString_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toBinaryString_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_reverse_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_reverse_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toHexString_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toHexString_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_int_java_lang_Long_numberOfTrailingZeros_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_numberOfTrailingZeros_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_max_long_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_max_long_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toOctalString_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toOctalString_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toString_long_int_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toString_long_int",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_rotateLeft_long_int_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_rotateLeft_long_int",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toUnsignedString_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toUnsignedString_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toString_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toString_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_int_java_lang_Long_signum_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_signum_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_java_lang_String_java_lang_Long_toString_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_java_lang_String_java_lang_Long_toString",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_int_java_lang_Long_numberOfLeadingZeros_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_int_java_lang_Long_numberOfLeadingZeros_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_double_java_lang_Long_doubleValue_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_double_java_lang_Long_doubleValue",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_int_java_lang_Long_intValue_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_int_java_lang_Long_intValue",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_long_java_lang_Long_lowestOneBit_long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_long_java_lang_Long_lowestOneBit_long",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_String_java_lang_Long_toUnsignedString_long_int_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_String_java_lang_Long_toUnsignedString_long_int",
				"Main.java", "../common");
		perform(conf);
	}
	
	@Test
	public void Long_public_static_java_lang_Long_java_lang_Long_getLong_java_lang_String_java_lang_Long_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Long_public_static_java_lang_Long_java_lang_Long_getLong_java_lang_String_java_lang_Long",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Boolean_public_boolean_java_lang_Boolean_booleanValue_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Boolean_public_boolean_java_lang_Boolean_booleanValue",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void GraphFragment_false_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"GraphFragment_false",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void ProjectionGridRoundTripper_exceptionprone_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"ProjectionGridRoundTripper_exceptionprone",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Float_public_boolean_java_lang_Float_isInfinite_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Float_public_boolean_java_lang_Float_isInfinite",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Float_public_boolean_java_lang_Float_isNaN_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Float_public_boolean_java_lang_Float_isNaN",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Float_public_static_boolean_java_lang_Float_isFinite_float_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Float_public_static_boolean_java_lang_Float_isFinite_float",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Float_public_static_boolean_java_lang_Float_isInfinite_float_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Float_public_static_boolean_java_lang_Float_isInfinite_float",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Float_public_static_boolean_java_lang_Float_isNaN_float_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Float_public_static_boolean_java_lang_Float_isNaN_float",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Double_public_boolean_java_lang_Double_isNaN_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Double_public_boolean_java_lang_Double_isNaN",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Double_public_static_boolean_java_lang_Double_isFinite_double_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Double_public_static_boolean_java_lang_Double_isFinite_double",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Double_public_static_boolean_java_lang_Double_isInfinite_double_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Double_public_static_boolean_java_lang_Double_isInfinite_double",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Double_public_static_boolean_java_lang_Double_isNaN_double_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Double_public_static_boolean_java_lang_Double_isNaN_double",
				"Main.java", "../common");
		perform(conf);
	}

	@Test
	public void Double_public_boolean_java_lang_Double_isInfinite_test() throws IOException {
		CronConfiguration conf = TestHelpers.assertCheckerWithConstantPropagation("svcomp",
				"Double_public_boolean_java_lang_Double_isInfinite",
				"Main.java", "../common");
		perform(conf);
	}
}

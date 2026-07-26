package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.java.constructs.classmetatype.LoadClass;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.VoidType;
import java.util.Map;

// NOTE: the value of this is the string that's in front of the `.class` (the class' name)

public class JavaClassLiteral extends Literal<String> {

	private static final Map<String, Type> types = Map.ofEntries(
			Map.entry("void", VoidType.INSTANCE),
			Map.entry("boolean", JavaBooleanType.INSTANCE),
			Map.entry("byte", JavaByteType.INSTANCE),
			Map.entry("char", JavaCharType.INSTANCE),
			Map.entry("double", JavaDoubleType.INSTANCE),
			Map.entry("float", JavaFloatType.INSTANCE),
			Map.entry("int", JavaIntType.INSTANCE),
			Map.entry("long", JavaLongType.INSTANCE),
			Map.entry("short", JavaShortType.INSTANCE));

	public JavaClassLiteral(
			CFG cfg,
			CodeLocation location,
			String value) {
		super(cfg, location, value, new JavaReferenceType(JavaClassType.getClassMetaType()));
	}

	@Override
	public String toString() {
		return "\"" + getValue() + "\"";
	}

	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {

		// TODO: simple names, for example: String.class should be valid

		// TODO: array types, for example int[].class should be valid

		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		Type t = getTypeFromStr(getValue());
		assert (t != null);

		LoadClass loadClass = new LoadClass(t, getValue(), cfg, location);
		AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, entryState, new ExpressionSet[0],
				expressions);

		return callState;
	}

	private Type getTypeFromStr(
			String clazzName) {
		clazzName = clazzName.replace('$', '.');
		if (types.containsKey(clazzName))
			return types.get(clazzName);
		return getProgram().getTypes().getType(clazzName);
	}
}

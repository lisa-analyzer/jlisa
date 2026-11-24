package it.unive.jlisa.program.java.constructs.character;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import java.util.Set;
import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;

public class CharacterEquals extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public CharacterEquals(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "equals", left, right);
	}

	public static CharacterEquals build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CharacterEquals(cfg, location, params[0], params[1]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		// equals(Object) holds only for a Character with the same value: the
		// argument might be of another class (e.g., an Integer), or null
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type character = JavaClassType.lookup("java.lang.Character");
		Set<Type> types = analysis.getRuntimeTypesOf(state, right, context, this);
		boolean unknown = types.isEmpty() || types.stream().anyMatch(Type::isUntyped);
		boolean characters = types.stream().anyMatch(t -> isCharacter(t, character));
		boolean others = types.stream().anyMatch(t -> !isCharacter(t, character));
		if (!unknown && !characters)
			return analysis.smallStepSemantics(state,
					new Constant(getProgram().getTypes().getBooleanType(), false, getLocation()), context,
					originating);
		if (unknown || others)
			// the executions where the argument is a Character are not
			// told apart from the other ones
			return analysis.smallStepSemantics(state,
					new PushAny(getProgram().getTypes().getBooleanType(), getLocation()), context, originating);

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(JavaCharType.INSTANCE, left, getLocation());
		AccessChild accessLeft = new AccessChild(JavaCharType.INSTANCE, derefLeft, var, getLocation());

		HeapDereference derefRight = new HeapDereference(JavaCharType.INSTANCE, right, getLocation());
		AccessChild accessRight = new AccessChild(JavaCharType.INSTANCE, derefRight, var, getLocation());

		it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
				getProgram().getTypes().getBooleanType(),
				accessLeft,
				accessRight,
				JavaCharacterEqualsOperator.INSTANCE,
				getLocation());
		return analysis.smallStepSemantics(state, equalsExpr, context, originating);
	}

	// whether the values of the type are Characters
	private static boolean isCharacter(
			Type t,
			Type character) {
		return t.isPointerType() && t.asPointerType().getInnerType().equals(character);
	}
}

package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;

public class StringSplit extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringSplit(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "split", left, right);
	}

	public static StringSplit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringSplit(cfg, location, params[0], params[1]);
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
		// FIXME: this is a temporary solution: we return an array with length
		// set to top but with
		// for elements, set to the top string
		JavaReferenceType stringArrayType = JavaArrayType.getStringArray();
		JavaClassType stringType = JavaClassType.getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		String src = ((SourceCodeLocation) getLocation()).getSourceFile();
		Expression unknown = stringArrayType.unknownValue(getCFG(), originating.getLocation());
		AnalysisState<A> splitArray = unknown.forwardSemantics(state, interprocedural, expressions);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression spl : splitArray.getExecutionExpressions()) {
			AccessChild ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 0, getLocation()), getLocation());

			// the 'value' property of strings
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", originating.getLocation());

			// first
			Expression unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 1),
					new JavaReferenceType(stringType), new Expression[0]);

			AnalysisState<A> unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			SymbolicExpression topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny()
					.get();
			AccessChild access = new AccessChild(stringType, topStringRef, var, getLocation());
			AnalysisState<
					A> sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, originating));

			// second
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 1, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 2),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, originating));

			// third
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 2, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 3),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, originating));

			// forth
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 3, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 4),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, originating));

		}

		return result.withExecutionExpressions(splitArray.getExecutionExpressions());
	}
}

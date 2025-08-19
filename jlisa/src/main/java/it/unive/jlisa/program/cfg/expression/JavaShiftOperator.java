package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;

import java.util.Collections;
import java.util.Set;

public class JavaShiftOperator extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	public JavaShiftOperator(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, ">>", inferType(left, right), left, right);
	}

	private static Type inferType(Expression left, Expression right) {
		Type leftType = left.getStaticType();
		Type rightType = right.getStaticType();

        if (leftType.isNumericType() && rightType.isNumericType()) {
            return leftType;
        }
		else return Untyped.INSTANCE;
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
					throws SemanticException {
		throw new SemanticException("Shift operator not supported");
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
}

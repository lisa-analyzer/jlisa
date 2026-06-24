package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaIsFieldDefinedOperator;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Modifier;

public class ClassGetField extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected ClassGetField(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "getField", left, right);
	}

	public static ClassGetField build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetField(cfg, location, params[0], params[1]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type intType = JavaIntType.INSTANCE;
		Type stringType = getProgram().getTypes().getStringType();
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type classMetaType = JavaClassType.getClassMetaType();

		// access class name (1st arg)
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		HeapDereference derefClassNameExpr = new HeapDereference(stringType, left, getLocation());
		AccessChild accessClassNameExpr = new AccessChild(stringType, derefClassNameExpr, nameVar, getLocation());

		// access field name (2nd arg)
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefFieldNameExpr = new HeapDereference(stringType, right, getLocation());
		AccessChild accessFieldNameExpr = new AccessChild(stringType, derefFieldNameExpr, valueVar, getLocation());

		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", getLocation());
		GlobalVariable typeVar = new GlobalVariable(Untyped.INSTANCE, "type", getLocation());
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", getLocation());

		// check if field actually exists in the given class
		it.unive.lisa.symbolic.value.BinaryExpression isFieldDefined = new it.unive.lisa.symbolic.value.BinaryExpression(
				stringType,
				accessClassNameExpr,
				accessFieldNameExpr,
				JavaIsFieldDefinedOperator.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, isFieldDefined, originating);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		if (sat != Satisfiability.NOT_SATISFIED) {

			// allocate the Field object
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(fieldMetaType),
					new Expression[0]);

			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			AnalysisState<A> tmp = callState.bottomExecution();

			for (SymbolicExpression expr : callState.getExecutionExpressions()) {

				// *field
				HeapDereference derefThisField = new HeapDereference(fieldMetaType, expr, getLocation());

				// assign field name

				// (*field)->name
				AccessChild accessThisFieldName = new AccessChild(new JavaReferenceType(stringType), derefThisField,
						nameVar, getLocation());

				AnalysisState<A> sem = analysis.assign(callState, accessThisFieldName, right, this);

				// assign field clazz

				// (*field)->clazz
				AccessChild accessThisFieldClazz = new AccessChild(new JavaReferenceType(classMetaType), derefThisField,
						clazzVar, getLocation());

				sem = analysis.assign(sem, accessThisFieldClazz, left, this);

				// assign field type

				Constant fieldTypeValue = new Constant(stringType, this.getType(), getLocation());

				// (*field)->type
				AccessChild accessThisFieldType = new AccessChild(new JavaReferenceType(classMetaType), derefThisField,
						typeVar, getLocation());

				// (*(*field)->type)->name
				HeapDereference derefFieldType = new HeapDereference(classMetaType, accessThisFieldType, getLocation());
				AccessChild dst = new AccessChild(stringType, derefFieldType, nameVar, getLocation());

				sem = analysis.assign(sem, dst, fieldTypeValue, this);

				// assign field modifiers

				Constant fieldModifiersValue = new Constant(JavaIntType.INSTANCE, this.getModifiers(), getLocation());

				// (*field)->modifiers
				dst = new AccessChild(intType, derefThisField, modifiersVar, getLocation());
				sem = analysis.assign(sem, dst, fieldModifiersValue, this);

				tmp = tmp.lub(sem);

				getMetaVariables().addAll(call.getMetaVariables());
				noExceptionState = tmp.withExecutionExpressions(callState.getExecutionExpressions());
			}
		}
		if (sat != Satisfiability.SATISFIED) {

			JavaClassType noSuchFieldType = JavaClassType.getNoSuchFieldException();

			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					noSuchFieldType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), noSuchFieldType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
			state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

			exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(noSuchFieldType.getReference(), originating), this);
		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private int getModifiers() {
		Global g = ReflectionCache.getLastField();

		boolean isInstance = g.isInstance();
		int modifiers = (isInstance) ? 0 : Modifier.STATIC;

		return modifiers;
	}

	private String getType() {
		Global g = ReflectionCache.getLastField();

		Type paramType = g.getStaticType();
		if (paramType.isReferenceType()) {
			paramType = paramType.asReferenceType().getInnerType();
		}

		String s = paramType.toString();
		return s;

	}

}

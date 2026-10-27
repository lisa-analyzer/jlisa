package it.unive.jlisa.checkers;

import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.asserts.AssertStatement;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.NaryStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The target of the violation witness of an uncaught runtime exception, that
 * is, the innermost statement or full expression where the exception is raised.
 * When an exception leaves a called method, LiSA moves it to the call, hence
 * the target is found by following the calls from the end of the main method to
 * the statement that raises the exception.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class RuntimeExceptionTarget {

	private RuntimeExceptionTarget() {
	}

	/**
	 * Returns the target of the violation of an uncaught runtime exception. It
	 * is known only when all the errors of the given state are runtime
	 * exceptions raised at the same statement.
	 *
	 * @param <A>   the type of the abstract states
	 * @param <D>   the type of the abstract domain
	 * @param tool  the tool of the check
	 * @param state the state at the end of the main method
	 *
	 * @return the target, or nothing if it is not known
	 *
	 * @throws SemanticException if a call cannot be resolved
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Optional<Statement> of(
			SemanticTool<A, D> tool,
			AnalysisState<A> state)
			throws SemanticException {
		// the smashed errors only keep the types of the exceptions, and the
		// errors at top do not tell where the exceptions are raised: no
		// witness is written, while the verdict of the analysis does not
		// change
		if (state.getErrors().isTop()
				|| (!state.getSmashedErrors().isBottom()
						&& (state.getSmashedErrors().isTop() || !state.getSmashedErrors().function.isEmpty())))
			return Optional.empty();

		Set<Statement> origins = new HashSet<>();
		Set<AnalysisState.Error> visited = new HashSet<>();
		for (var error : state.getErrors())
			if (!isRuntimeException(error.getKey().getType())
					|| !collectOrigins(tool, error.getKey(), origins, visited))
				return Optional.empty();
		if (origins.size() != 1)
			return Optional.empty();
		return Optional.of(fullExpression(origins.iterator().next()));
	}

	/**
	 * Adds to the given set the statements where the given error is raised.
	 * When an error leaves a called method, LiSA moves it to the call. In this
	 * case, the errors with the same type at the exit of the called methods are
	 * followed, until a statement that is not a call to an analyzed method. An
	 * error of a call that does not leave any called method is raised by the
	 * call itself (e.g., on a null receiver), as calls to methods that are not
	 * in the program raise no error.
	 *
	 * @param <A>     the type of the abstract states
	 * @param <D>     the type of the abstract domain
	 * @param tool    the tool of the check
	 * @param error   the error
	 * @param origins the statements where the errors are raised
	 * @param visited the errors already followed
	 *
	 * @return {@code false} if the origin of the error is not known, e.g.,
	 *             because a call cannot be resolved
	 *
	 * @throws SemanticException if a call cannot be resolved
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean collectOrigins(
			SemanticTool<A, D> tool,
			AnalysisState.Error error,
			Set<Statement> origins,
			Set<AnalysisState.Error> visited)
			throws SemanticException {
		if (!visited.add(error))
			return true;

		Statement thrower = error.getThrower();
		if (!(thrower instanceof UnresolvedCall call)) {
			origins.add(thrower);
			return true;
		}

		Collection<AnalyzedCFG<A>> results = tool.getResultOf(call.getCFG());
		if (results == null)
			return false;

		boolean followed = false;
		for (AnalyzedCFG<A> result : results) {
			if (receiverMayBeNull(tool, call, result, error))
				// the call raises the error itself
				origins.add(call);
			Call resolved = tool.getResolvedVersion(call, result);
			if (resolved == null)
				return false;
			Collection<Call> targets = resolved instanceof MultiCall multi ? multi.getCalls() : List.of(resolved);
			for (Call target : targets)
				if (target instanceof CFGCall cfgCall)
					for (CFG callee : cfgCall.getTargetedCFGs()) {
						Collection<AnalyzedCFG<A>> calleeResults = tool.getResultOf(callee);
						if (calleeResults == null)
							return false;
						for (AnalyzedCFG<A> calleeResult : calleeResults)
							for (var calleeError : calleeResult.getExitState().getErrors())
								if (calleeError.getKey().getType().equals(error.getType())) {
									followed = true;
									if (!collectOrigins(tool, calleeError.getKey(), origins, visited))
										return false;
								}
					}
				else
					// a library or open call raises its errors itself
					origins.add(call);
		}
		if (!followed)
			// the error is raised by the call itself
			origins.add(call);
		return true;
	}

	/**
	 * Returns whether the given error can be raised by the given call itself,
	 * and not by the called method. A call raises by itself only a
	 * {@link NullPointerException}, when its receiver is null (JLS 15.12.4.4,
	 * as modeled by
	 * {@link it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall}), while
	 * any other runtime exception of a call comes from the called method.
	 * Hence, this holds when the error is a {@link NullPointerException} and
	 * the call is an instance call whose receiver can be null in the given
	 * result. As in the call, the receiver is checked after the evaluation of
	 * all the arguments.
	 *
	 * @param <A>    the type of the abstract states
	 * @param <D>    the type of the abstract domain
	 * @param tool   the tool of the check
	 * @param call   the call
	 * @param result the result of the method containing the call
	 * @param error  the error
	 *
	 * @return {@code true} if the call can raise the error itself
	 *
	 * @throws SemanticException if the types of the receiver cannot be computed
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean receiverMayBeNull(
			SemanticTool<A, D> tool,
			UnresolvedCall call,
			AnalyzedCFG<A> result,
			AnalysisState.Error error)
			throws SemanticException {
		Expression[] parameters = call.getParameters();
		if (call.getCallType() != CallType.INSTANCE
				|| parameters.length == 0
				|| !error.getType().equals(JavaClassType.getNullPointerExceptionType().getReference()))
			return false;
		// the state after the last argument, where the call checks the
		// receiver
		AnalysisState<A> state = result.getAnalysisStateAfter(parameters[parameters.length - 1]);
		AnalysisState<A> receiver = result.getAnalysisStateAfter(parameters[0]);
		for (SymbolicExpression value : receiver.getExecutionExpressions())
			for (Type type : tool.getAnalysis().getRuntimeTypesOf(state, value, call))
				if (type.isPointerType() && type.asPointerType().getInnerType().isNullType())
					return true;
		return false;
	}

	/**
	 * Returns whether the given type is a runtime exception, that is, a
	 * subclass of {@code java.lang.RuntimeException}.
	 *
	 * @param type the type
	 *
	 * @return {@code true} if the type is a runtime exception
	 */
	private static boolean isRuntimeException(
			Type type) {
		if (!type.isReferenceType() || !type.asReferenceType().getInnerType().isUnitType())
			return false;
		CompilationUnit unit = type.asReferenceType().getInnerType().asUnitType().getUnit();
		return unit instanceof ClassUnit cls
				&& cls.isInstanceOf(JavaClassType.getRuntimeExceptionType().getUnit());
	}

	/**
	 * Returns the innermost statement or full expression containing the given
	 * statement. It is the root statement of the given one, except when the
	 * root is a statement that is not an expression in Java (a return, a throw,
	 * an assertion, or the declaration of a variable with its initializer). In
	 * this case, the expression of the root containing the given statement is a
	 * full expression on its own.
	 *
	 * @param statement the statement
	 *
	 * @return the innermost statement or full expression
	 */
	private static Statement fullExpression(
			Statement statement) {
		Statement root = statement instanceof Expression expression ? expression.getRootStatement() : statement;
		boolean javaStatement = root instanceof NaryStatement
				|| root instanceof AssertStatement
				|| (root instanceof JavaAssignment assignment && assignment.isInitializer());
		if (root == statement || !javaStatement)
			return root;

		// the expression of the root that contains the given statement
		Statement child = statement;
		while (((Expression) child).getParentStatement() != root)
			child = ((Expression) child).getParentStatement();

		// when an initializer has another type than its variable, the frontend
		// wraps it in an implicit cast with a synthetic location, hence the
		// target is the initializer inside it
		if (!(child.getLocation() instanceof SourceCodeLocation)
				&& child instanceof NaryExpression conversion
				&& conversion.getSubExpressions().length == 1)
			return conversion.getSubExpressions()[0];
		return child;
	}
}

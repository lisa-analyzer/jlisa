package it.unive.jlisa.interprocedural.callgraph;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.NaryStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A check for recursion in a program, before the analysis runs. It builds a
 * graph of the methods of the program from the names of the calls in their
 * bodies, without resolving the calls.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class Recursion {

	private Recursion() {
	}

	/**
	 * Returns whether the given program may contain recursion: whether the
	 * graph of its methods, with an edge from a method to every method with the
	 * name of a call in its body, has a cycle. Methods are identified by name
	 * only, hence the graph has more edges than the real call graph.
	 *
	 * @param program the program
	 *
	 * @return {@code false} if the program has no recursion
	 */
	public static boolean mayBeRecursive(
			Program program) {
		Map<String, Set<String>> calls = new HashMap<>();
		for (CFG cfg : program.getAllCFGs()) {
			Set<String> names = calls.computeIfAbsent(cfg.getDescriptor().getName(), k -> new HashSet<>());
			for (Statement statement : cfg.getNodes())
				collectCalls(statement, names);
		}
		Set<String> done = new HashSet<>();
		for (String method : calls.keySet())
			if (hasCycle(method, calls, new HashSet<>(), done))
				return true;
		return false;
	}

	private static void collectCalls(
			Statement statement,
			Set<String> names) {
		// TODO the calls built during the analysis are not in the CFGs: only
		// the constructor calls of new are added here, hence a recursion
		// through a static initializer or through reflection is not found
		if (statement instanceof Call call)
			names.add(call.getTargetName());
		else if (statement instanceof JavaNewObj newObj)
			// the constructor call of new is built during the analysis: it
			// targets the constructor, named after the simple name of the class
			names.add(constructorName(newObj));
		Expression[] subexpressions = statement instanceof NaryExpression nary ? nary.getSubExpressions()
				: statement instanceof NaryStatement nary ? nary.getSubExpressions() : new Expression[0];
		for (Expression subexpression : subexpressions)
			collectCalls(subexpression, names);
	}

	private static boolean hasCycle(
			String method,
			Map<String, Set<String>> calls,
			Set<String> path,
			Set<String> done) {
		if (path.contains(method))
			return true;
		if (done.contains(method) || !calls.containsKey(method))
			return false;
		path.add(method);
		for (String callee : calls.get(method))
			if (hasCycle(callee, calls, path, done))
				return true;
		path.remove(method);
		done.add(method);
		return false;
	}

	private static String constructorName(
			JavaNewObj newObj) {
		String className = ((JavaReferenceType) newObj.getStaticType()).getInnerType().toString();
		return className.substring(className.lastIndexOf('.') + 1);
	}
}

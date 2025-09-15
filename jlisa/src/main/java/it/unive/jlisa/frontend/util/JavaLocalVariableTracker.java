package it.unive.jlisa.frontend.util;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Untyped;
import java.util.*;
import java.util.Map.Entry;

public class JavaLocalVariableTracker {

	public static class LocalVariable {
		private final CodeLocation location;
		private final Statement scopeStart;
		private final Annotations annotations;

		private LocalVariable(
				CodeLocation location,
				Statement scopeStart,
				Annotations annotations) {
			this.location = location;
			this.scopeStart = scopeStart;
			this.annotations = annotations;
		}

		public CodeLocation getLocation() {
			return location;
		}

		public Statement getScopeStart() {
			return scopeStart;
		}

		public Annotations getAnnotations() {
			return annotations;
		}

	}

	private final CodeMemberDescriptor descriptor;

	private final List<Map<String, LocalVariable>> visibleIds;

	private Map<String, LocalVariable> latestScope;

	private int varIndex;

	/**
	 * Creates a new local variable tracker for the given {@code cfg}. All
	 * parameters of the given graph are immediately added to the root scope.
	 *
	 * @param cfg        the control flow graph
	 * @param descriptor the descriptor
	 */
	public JavaLocalVariableTracker(
			CFG cfg,
			CodeMemberDescriptor descriptor) {
		this.descriptor = descriptor;
		visibleIds = new LinkedList<>();
		latestScope = new HashMap<>();
		for (VariableTableEntry par : descriptor.getVariables())
			latestScope.put(
					par.getName(),
					new LocalVariable(par.getLocation(), par.createReference(cfg), par.getAnnotations()));
		visibleIds.add(latestScope);
		varIndex = descriptor.getVariables().size();
	}

	/**
	 * Enters a new scope, which is initially empty. This scope will be used to
	 * track new local variables.
	 */
	public void enterScope() {
		latestScope = new HashMap<>();
		visibleIds.add(latestScope);
	}

	/**
	 * Exits the current scope, adding all the variables defined in this scope
	 * to the descriptor. The given {@code closing} statement is used to
	 * determine the end of the scope, and is used to set the scope end of the
	 * variables. The scope is then restored to parent one, which is the one
	 * that was active before the one being exited.
	 *
	 * @param closing the statement that closes the scope
	 *
	 * @throws IllegalStateException if no scopes are currently active
	 */
	public void exitScope(
			Statement closing) {
		if (visibleIds.isEmpty())
			throw new IllegalStateException("Cannot exit scope: no scopes are currently active");

		for (Entry<String, LocalVariable> id : latestScope.entrySet())
			descriptor.addVariable(
					new VariableTableEntry(
							id.getValue().location,
							varIndex++,
							id.getValue().scopeStart,
							closing,
							id.getKey(),
							Untyped.INSTANCE,
							id.getValue().annotations));

		visibleIds.remove(visibleIds.size() - 1);
		latestScope = visibleIds.get(visibleIds.size() - 1);
	}

	/**
	 * Checks whether a variable with the given name is visible in the current
	 * scope or in any of the outer scopes.
	 *
	 * @param name the name of the variable to check
	 *
	 * @return {@code true} if the variable is visible, {@code false} otherwise
	 */
	public boolean hasVariable(
			String name) {
		for (Map<String, LocalVariable> scope : visibleIds)
			if (scope.containsKey(name))
				return true;
		return false;
	}

	/**
	 * Tracks a new variable with the given name, definition, and annotations.
	 * The definition is the statement that defines this variable, and is used
	 * to determine the scope of the variable.
	 *
	 * @param name        the name of the variable
	 * @param definition  the statement that defines this variable
	 * @param annotations the annotations associated with this variable
	 */
	public void addVariable(
			String name,
			Statement definition,
			Annotations annotations) {
		latestScope.put(
				name,
				new LocalVariable(
						definition.getLocation(),
						definition instanceof Expression ? ((Expression) definition).getRootStatement() : definition,
						annotations));
	}

	/**
	 * Yields a copy of the latest scope.
	 *
	 * @return map the copy of the latest scope.
	 */
	public Map<String, LocalVariable> getLatestScope() {
		return new HashMap<>(latestScope);
	}

	public LocalVariable getLocalVariable(
			String identifier) {
		ListIterator<Map<String, LocalVariable>> iterator = visibleIds.listIterator(visibleIds.size());
		while (iterator.hasPrevious()) {
			Map<String, LocalVariable> scope = iterator.previous();
			if (scope.containsKey(identifier)) {
				return scope.get(identifier);
			}
		}
		return null;
	}

}
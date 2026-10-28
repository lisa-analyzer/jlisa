package it.unive.jlisa.lattices;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Var;
import it.unive.jlisa.analysis.value.NativeMemory;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.apron.Apron;
import it.unive.lisa.analysis.apron.ApronAccess;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A state of {@link Apron} together with the identifiers it tracks. Apron knows
 * the variables only by name: the identifiers are kept here to move the
 * variables across calls as LiSA does (the locals of the caller are renamed
 * when a call starts, the locals of the callee are dropped when it ends,
 * globals and heap locations are kept), and to forget them by their kind.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ScopedApron
		implements
		ValueLattice<ScopedApron> {

	private final Apron apron;

	private final Map<String, Identifier> ids;

	/**
	 * Builds the top state.
	 */
	public ScopedApron() {
		this(new Apron(), Collections.emptyMap());
	}

	/**
	 * Builds a state.
	 *
	 * @param apron the state of Apron
	 * @param ids   the identifiers tracked by {@code apron}, by name
	 */
	public ScopedApron(
			Apron apron,
			Map<String, Identifier> ids) {
		this.apron = apron;
		this.ids = ids;
	}

	/**
	 * Returns the state of Apron.
	 *
	 * @return the state of Apron
	 */
	public Apron getApron() {
		return apron;
	}

	/**
	 * Returns this state with the given state of Apron, that also tracks the
	 * given identifiers.
	 *
	 * @param state   the new state of Apron
	 * @param tracked the identifiers added by the operation that produced
	 *                    {@code state}
	 *
	 * @return the new state
	 */
	public ScopedApron with(
			Apron state,
			Iterable<Identifier> tracked) {
		Map<String, Identifier> m = new HashMap<>(ids);
		for (Identifier id : tracked)
			m.put(id.getName(), id);
		return new ScopedApron(state, m);
	}

	private static Map<String, Identifier> union(
			Map<String, Identifier> a,
			Map<String, Identifier> b) {
		if (a.isEmpty())
			return b;
		if (b.isEmpty())
			return a;
		Map<String, Identifier> m = new HashMap<>(a);
		m.putAll(b);
		return m;
	}

	@Override
	public boolean lessOrEqual(
			ScopedApron other)
			throws SemanticException {
		NativeMemory.operation();
		return apron.lessOrEqual(other.apron);
	}

	@Override
	public ScopedApron lub(
			ScopedApron other)
			throws SemanticException {
		NativeMemory.operation();
		return new ScopedApron(apron.lub(other.apron), union(ids, other.ids));
	}

	@Override
	public ScopedApron glb(
			ScopedApron other)
			throws SemanticException {
		NativeMemory.operation();
		return new ScopedApron(apron.glb(other.apron), union(ids, other.ids));
	}

	@Override
	public ScopedApron widening(
			ScopedApron other)
			throws SemanticException {
		NativeMemory.operation();
		return new ScopedApron(apron.widening(other.apron), union(ids, other.ids));
	}

	@Override
	public ScopedApron narrowing(
			ScopedApron other)
			throws SemanticException {
		NativeMemory.operation();
		return new ScopedApron(apron.narrowing(other.apron), union(ids, other.ids));
	}

	@Override
	public ScopedApron top() {
		return new ScopedApron(apron.top(), Collections.emptyMap());
	}

	@Override
	public ScopedApron bottom() {
		return new ScopedApron(apron.bottom(), Collections.emptyMap());
	}

	@Override
	public boolean isTop() {
		return apron.isTop();
	}

	@Override
	public boolean isBottom() {
		return apron.isBottom();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return ids.containsKey(id.getName());
	}

	@Override
	public ScopedApron forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (!ids.containsKey(id.getName()))
			return this;
		Map<String, Identifier> m = new HashMap<>(ids);
		m.remove(id.getName());
		return new ScopedApron(remove(apron, List.of(id)), m);
	}

	@Override
	public ScopedApron forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		List<Identifier> forgotten = new ArrayList<>();
		for (Identifier id : ids.values())
			if (test.test(id))
				forgotten.add(id);
		return forgetIdentifiers(forgotten, pp);
	}

	@Override
	public ScopedApron forgetIdentifiers(
			Iterable<Identifier> forgotten,
			ProgramPoint pp)
			throws SemanticException {
		Map<String, Identifier> m = new HashMap<>(ids);
		List<Identifier> known = new ArrayList<>();
		for (Identifier id : forgotten)
			if (m.remove(id.getName()) != null)
				known.add(id);
		if (known.isEmpty())
			return this;
		return new ScopedApron(remove(apron, known), m);
	}

	@Override
	public ScopedApron store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (!ids.containsKey(source.getName()) || !hasVariable(apron, source))
			return this;
		Map<String, Identifier> m = new HashMap<>(ids);
		m.put(target.getName(), target);
		return new ScopedApron(copy(apron, target, source, null), m);
	}

	@Override
	public ScopedApron pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return moveScope(token, pp, true);
	}

	@Override
	public ScopedApron popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return moveScope(token, pp, false);
	}

	// each identifier becomes what LiSA makes of it when the scope changes:
	// the same identifier (kept), another one (renamed, moving its relations),
	// or none (forgotten)
	private ScopedApron moveScope(
			ScopeToken token,
			ProgramPoint pp,
			boolean push)
			throws SemanticException {
		if (ids.isEmpty() || apron.isBottom())
			return this;
		Apron state = apron;
		Map<String, Identifier> m = new HashMap<>();
		List<Identifier> forgotten = new ArrayList<>();
		for (Identifier id : ids.values()) {
			SymbolicExpression moved = push ? id.pushScope(token, pp) : id.popScope(token, pp);
			if (!(moved instanceof Identifier)) {
				forgotten.add(id);
				continue;
			}
			Identifier target = (Identifier) moved;
			if (!target.getName().equals(id.getName()) && hasVariable(state, id))
				state = copy(state, target, id, pp);
			m.put(target.getName(), target);
		}
		// the old names are forgotten after all the renamings, since a new name
		// might be the old name of another identifier
		for (Identifier id : ids.values())
			if (!m.containsKey(id.getName()))
				forgotten.add(id);
		if (!forgotten.isEmpty())
			state = remove(state, forgotten);
		return new ScopedApron(state, m);
	}

	// target = source: unlike Apron.store, adds target to the environment if
	// it is missing (an assignment of an identifier needs no oracle)
	private static Apron copy(
			Apron state,
			Identifier target,
			Identifier source,
			ProgramPoint pp)
			throws SemanticException {
		return state.assign(state, target, source, pp, null);
	}

	/**
	 * Returns this state with at most the given number of variables in Apron:
	 * when there are more, the ones of the identifiers not in {@code keep} are
	 * removed (a polyhedron can grow exponentially with its variables).
	 *
	 * @param keep the identifiers to keep (e.g., the ones of the current
	 *                 operation)
	 * @param max  the maximum number of variables
	 *
	 * @return the state, with at most {@code max} variables (or the ones in
	 *             {@code keep}, if they are more)
	 *
	 * @throws SemanticException if Apron fails
	 */
	public ScopedApron limit(
			Set<Identifier> keep,
			int max)
			throws SemanticException {
		if (apron.isBottom() || apron.getApronState().getEnvironment().getSize() <= max)
			return this;
		Set<String> kept = new HashSet<>();
		for (Identifier id : keep)
			kept.add(id.getName());
		Map<String, Identifier> m = new HashMap<>();
		List<Identifier> removed = new ArrayList<>();
		for (Identifier id : ids.values())
			if (kept.contains(id.getName()))
				m.put(id.getName(), id);
			else
				removed.add(id);
		List<String> names = new ArrayList<>();
		for (Var v : apron.getApronState().getEnvironment().getVars())
			if (!kept.contains(v.toString()))
				names.add(v.toString());
		return new ScopedApron(removeNames(apron, names), m);
	}

	// the given state without the variables of the given identifiers: Apron
	// only forgets a variable, keeping its dimension, and the polyhedra grow
	// with the dimensions; a missing variable stands for any value, as a
	// forgotten one
	private static Apron remove(
			Apron state,
			Iterable<Identifier> removed)
			throws SemanticException {
		List<String> names = new ArrayList<>();
		for (Identifier id : removed)
			names.add(id.getName());
		return removeNames(state, names);
	}

	private static Apron removeNames(
			Apron state,
			List<String> names)
			throws SemanticException {
		if (state.isBottom())
			return state;
		Abstract1 abs = state.getApronState();
		Environment env = abs.getEnvironment();
		// a set: the same name might be given more than once
		Set<String> present = new LinkedHashSet<>();
		for (String n : names)
			if (env.hasVar(n))
				present.add(n);
		if (present.isEmpty())
			return state;
		try {
			Environment smaller = env.remove(present.toArray(new String[0]));
			return ApronAccess.of(abs.changeEnvironmentCopy(abs.getCreationManager(), smaller, false));
		} catch (ApronException e) {
			throw new SemanticException("Apron error while removing variables", e);
		}
	}

	// whether the environment of the given state has the variable of the
	// given identifier (Apron drops the variables that are unconstrained in
	// some operations, e.g., in a least upper bound)
	private static boolean hasVariable(
			Apron state,
			Identifier id) {
		return state.getApronState().getEnvironment().hasVar(id.getName());
	}

	@Override
	public StructuredRepresentation representation() {
		return apron.representation();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		ScopedApron other = (ScopedApron) obj;
		return Objects.equals(apron, other.apron) && Objects.equals(ids, other.ids);
	}

	@Override
	public int hashCode() {
		return Objects.hash(apron, ids);
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}

package it.unive.jlisa.interprocedural.callgraph;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.analysis.symbols.Aliases;
import it.unive.lisa.analysis.symbols.NameSymbol;
import it.unive.lisa.analysis.symbols.QualifiedNameSymbol;
import it.unive.lisa.analysis.symbols.QualifierSymbol;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.interprocedural.callgraph.BaseCallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A call graph implementation tailored for resolving Java method calls.
 * <p>
 * This class handles both static (non-instance) and instance call resolution,
 * using type information, aliasing, and hierarchy traversal strategies to
 * identify the most appropriate target(s) for a given {@link UnresolvedCall}.
 * </p>
 * <p>
 * Resolution relies on finding the "closest" matching target according to
 * method signatures, parameter types, and type hierarchies. If multiple equally
 * good matches are found, a {@link CallResolutionException} is thrown.
 * </p>
 */
public abstract class JavaCallGraph extends BaseCallGraph {

	/**
	 * Resolves a non-instance (static) call to its possible targets.
	 * <p>
	 * The resolution process works as follows:
	 * <ol>
	 * <li>Looks up the {@link CompilationUnit} corresponding to the call's
	 * qualifier (e.g., the class in which the method is declared).</li>
	 * <li>Traverses the class hierarchy starting from that unit using the
	 * program's {@link HierarcyTraversalStrategy}.</li>
	 * <li>For each candidate {@link CodeMember}, checks whether it is a valid
	 * target using
	 * {@link #isATarget(UnresolvedCall, SymbolAliasing, CodeMember, Set[], boolean)}.</li>
	 * <li>Computes the "distance" of the candidate from a perfect match using
	 * {@link #distanceFromPerfectTarget(UnresolvedCall, CodeMember, boolean)}.</li>
	 * <li>Keeps track of the best match (lowest distance). If a distance of 0
	 * (perfect match) is found, resolution stops immediately and that target is
	 * returned.</li>
	 * <li>If multiple candidates have the same best distance greater than 0, a
	 * {@link CallResolutionException} is thrown due to ambiguity.</li>
	 * </ol>
	 * </p>
	 *
	 * @param call     the unresolved call to resolve
	 * @param types    the possible runtime types of the call's parameters
	 * @param targets  the collection where resolved {@link CFG} targets will be
	 *                     stored
	 * @param natives  the collection where resolved {@link NativeCFG} targets
	 *                     will be stored
	 * @param aliasing aliasing information that may affect resolution
	 *
	 * @throws CallResolutionException if multiple equally suitable targets are
	 *                                     found
	 */
	@Override
	public void resolveNonInstance(
			UnresolvedCall call,
			Set<Type>[] types,
			Collection<CFG> targets,
			Collection<NativeCFG> natives,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		CompilationUnit targetUnit = JavaClassType.lookup(call.getQualifier()).getUnit();
		HierarchyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
		Set<CompilationUnit> seen = new HashSet<>();
		int lowestDistance = Integer.MAX_VALUE;
		Collection<CFG> bestTargets = new LinkedHashSet<>();
		Collection<NativeCFG> bestNatives = new LinkedHashSet<>();

		for (CompilationUnit cu : strategy.traverse(call, targetUnit)) {
			if (!seen.add(cu))
				continue;
			for (CodeMember cm : cu.getCodeMembers()) {
				if (!isATarget(call, aliasing, cm, types, false))
					continue;
				int distance = distanceFromPerfectTarget(call, cm, false);
				if (distance < 0)
					continue; // incomparable
				if (distance < lowestDistance) {
					lowestDistance = distance;
					bestTargets.clear();
					bestNatives.clear();
					addTarget(cm, bestTargets, bestNatives);
					if (distance == 0) {
						targets.addAll(bestTargets);
						natives.addAll(bestNatives);
						return;
					}
				} else if (distance == lowestDistance) {
					throw new CallResolutionException(
							"Multiple call targets for call " + call + " found in " + targetUnit +
									": " + bestTargets + " " + bestNatives);
				}
			}
		}
		targets.addAll(bestTargets);
		natives.addAll(bestNatives);
	}

	/**
	 * Resolves an instance call (a method call invoked on an object) to its
	 * possible targets.
	 * <p>
	 * The resolution process works as follows:
	 * <ol>
	 * <li>Ensures that the call has at least one parameter, which represents
	 * the receiver (the object on which the method is invoked).</li>
	 * <li>Determines the possible runtime {@link Type}s of the receiver
	 * expression, and retrieves the corresponding {@link CompilationUnit}s
	 * using {@link #getReceiverCompilationUnit(Type)}.</li>
	 * <li>For each receiver type, traverses the class hierarchy of its unit
	 * using the program's {@link HierarcyTraversalStrategy}.</li>
	 * <li>For each candidate {@link CodeMember}, checks whether it is a valid
	 * instance target using
	 * {@link #isATarget(UnresolvedCall, SymbolAliasing, CodeMember, Set[], boolean)}.</li>
	 * <li>Computes the "distance" of the candidate from a perfect match using
	 * {@link #distanceFromPerfectTarget(UnresolvedCall, CodeMember, boolean)}.</li>
	 * <li>Keeps track of the best match (lowest distance). If a distance of 0
	 * (perfect match) is found, resolution for that receiver stops
	 * immediately.</li>
	 * <li>If multiple candidates have the same best distance greater than 0, a
	 * {@link CallResolutionException} is thrown due to ambiguity.</li>
	 * <li>All best matches across possible receiver types are added to the
	 * {@code targets} and {@code natives} collections.</li>
	 * </ol>
	 * </p>
	 *
	 * @param call     the unresolved instance call to resolve
	 * @param types    the possible runtime types of the call's parameters,
	 *                     where {@code types[0]} corresponds to the receiver
	 * @param targets  the collection where resolved {@link CFG} targets will be
	 *                     stored
	 * @param natives  the collection where resolved {@link NativeCFG} targets
	 *                     will be stored
	 * @param aliasing aliasing information that may affect resolution
	 *
	 * @throws CallResolutionException if the call has no receiver, or if
	 *                                     multiple equally suitable targets are
	 *                                     found
	 */
	@Override
	public void resolveInstance(
			UnresolvedCall call,
			Set<Type>[] types,
			Collection<CFG> targets,
			Collection<NativeCFG> natives,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		if (call.getParameters().length == 0)
			throw new CallResolutionException(
					"An instance call should have at least one parameter as receiver");
		Expression receiver = call.getParameters()[0];

		for (Type recType : getPossibleTypesOfReceiver(receiver, types[0])) {
			CompilationUnit unit = getReceiverCompilationUnit(recType);
			if (unit == null)
				continue;
			HierarchyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
			Set<CompilationUnit> seen = new HashSet<>();
			int lowestDistance = Integer.MAX_VALUE;
			Collection<CFG> bestTargets = new LinkedHashSet<>();
			Collection<NativeCFG> bestNatives = new LinkedHashSet<>();
			boolean foundPerfect = false;

			for (CompilationUnit cu : strategy.traverse(call, unit)) {
				if (!seen.add(cu))
					continue;
				for (CodeMember cm : cu.getInstanceCodeMembers(false)) {
					if (!isATarget(call, aliasing, cm, types, true))
						continue;
					int distance = distanceFromPerfectTarget(call, cm, true);
					if (distance < 0)
						continue;
					if (distance < lowestDistance) {
						lowestDistance = distance;
						bestTargets.clear();
						bestNatives.clear();
						addTarget(cm, bestTargets, bestNatives);
						foundPerfect = (distance == 0);
						if (foundPerfect)
							break;
					} else if (distance == lowestDistance) {
						throw new CallResolutionException(
								"Multiple call targets for call " + call + " found in " + unit +
										": " + bestTargets + " " + bestNatives);
					}
				}
				if (foundPerfect)
					break;
			}
			targets.addAll(bestTargets);
			natives.addAll(bestNatives);
		}
	}

	/**
	 * Returns the {@link CompilationUnit} associated with a receiver type.
	 * <p>
	 * Supports both unit types and pointer-to-unit types.
	 * </p>
	 *
	 * @param receiverType the type of the receiver
	 *
	 * @return the compilation unit of the receiver, or {@code null} if
	 *             unavailable
	 */
	public CompilationUnit getReceiverCompilationUnit(
			Type receiverType) {
		if (receiverType.isUnitType())
			return receiverType.asUnitType().getUnit();
		if (receiverType.isPointerType() && receiverType.asPointerType().getInnerType().isUnitType())
			return receiverType.asPointerType().getInnerType().asUnitType().getUnit();
		return null;
	}

	/**
	 * Adds a resolved code member to the appropriate collection based on
	 * whether it is a {@link CFG} or a {@link NativeCFG}.
	 *
	 * @param cm            the code member
	 * @param cfgTargets    the collection for CFG targets
	 * @param nativeTargets the collection for native CFG targets
	 */
	private void addTarget(
			CodeMember cm,
			Collection<CFG> cfgTargets,
			Collection<NativeCFG> nativeTargets) {
		if (cm instanceof CFG cmCFG)
			cfgTargets.add(cmCFG);
		else
			nativeTargets.add((NativeCFG) cm);
	}

	/**
	 * Checks whether a given code member is a valid target for a call.
	 *
	 * @param call     the unresolved call
	 * @param aliasing aliasing information
	 * @param cm       the candidate code member
	 * @param types    the possible runtime types of the call's parameters
	 * @param instance whether the call is an instance call
	 *
	 * @return {@code true} if the code member is a valid target, {@code false}
	 *             otherwise
	 */
	private boolean isATarget(
			UnresolvedCall call,
			SymbolAliasing aliasing,
			CodeMember cm,
			Set<Type>[] types,
			boolean instance) {
		CodeMemberDescriptor descr = cm.getDescriptor();
		if (instance != descr.isInstance() || cm instanceof AbstractCodeMember)
			return false;

		String qualifier = descr.getUnit().getName();
		String name = descr.getName();

		boolean target = false;
		if (aliasing != null) {
			if (matchesAlias(call, aliasing, name, qualifier)) {
				target = true;
			}
		}
		if (!target)
			target = matchCodeMemberName(call, qualifier, name);
		ParameterMatchingStrategy strategy = call.getProgram().getFeatures().getMatchingStrategy();
		return target && strategy.matches(call, descr.getFormals(), call.getParameters(), types);
	}

	/**
	 * Checks whether the given call matches any known aliases for a member.
	 *
	 * @param call      the unresolved call
	 * @param aliasing  aliasing information
	 * @param name      the original method name
	 * @param qualifier the original qualifier (class or namespace)
	 *
	 * @return {@code true} if the call matches an alias, {@code false}
	 *             otherwise
	 */
	private boolean matchesAlias(
			UnresolvedCall call,
			SymbolAliasing aliasing,
			String name,
			String qualifier) {
		Aliases nAlias = aliasing.getState(new NameSymbol(name));
		Aliases qAlias = aliasing.getState(new QualifierSymbol(qualifier));
		Aliases qnAlias = aliasing.getState(new QualifiedNameSymbol(qualifier, name));

		if (!qnAlias.isEmpty()) {
			for (QualifiedNameSymbol alias : qnAlias.castElements(QualifiedNameSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), alias.getName()))
					return true;
		}
		if (!qAlias.isEmpty()) {
			for (QualifierSymbol alias : qAlias.castElements(QualifierSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), name))
					return true;
		}
		if (!nAlias.isEmpty()) {
			for (NameSymbol alias : nAlias.castElements(NameSymbol.class))
				if (matchCodeMemberName(call, qualifier, alias.getName()))
					return true;
		}
		return false;
	}

	/**
	 * Computes the "distance" between a candidate code member and a perfect
	 * match for a call.
	 * <p>
	 * Distance reflects type compatibility: zero means an exact match, higher
	 * values mean progressively less exact matches, and negative means
	 * incomparable.
	 * </p>
	 *
	 * @param call     the unresolved call
	 * @param cm       the candidate code member
	 * @param instance whether the call is an instance call
	 *
	 * @return the computed distance, or {@code -1} if incomparable
	 */
	private int distanceFromPerfectTarget(
			UnresolvedCall call,
			CodeMember cm,
			boolean instance) {
		int distance = 0;
		int startIdx = instance ? 1 : 0;
		Expression[] params = call.getParameters();
		CodeMemberDescriptor descriptor = cm.getDescriptor();
		for (int i = startIdx; i < params.length; i++) {
			Expression parameter = params[i];
			Type paramType = parameter.getStaticType();
			Type formalType = descriptor.getFormals()[i].getStaticType();
			if (formalType instanceof Untyped) {
				return 0;
			}
			if (paramType instanceof Untyped) {
				return 0; // It is being evaluated with the
							// CustomJavaLikeMatchingStrategy for now.
			}
			if (paramType instanceof JavaNumericType numericParam) {
				if (formalType instanceof JavaNumericType numericFormal) {
					int paramDist = numericParam.distance(numericFormal);
					if (paramDist < 0)
						return -1; // incomparable
					distance += paramDist;
				} else {
					return -1;
				}
			} else if (paramType.isBooleanType() && formalType.isBooleanType()) {
				return 0;
			} else if (JavaClassType.isWrapperOf(formalType, paramType)) {
				// boxing
				distance += 1;
			} else if (JavaClassType.isWrapperOf(paramType, formalType)) {
				// unboxing
				distance += 1;
			} else if (paramType instanceof ReferenceType refTypeParam
					&& formalType instanceof ReferenceType refTypeFormal) {
				if (refTypeParam.getInnerType().isNullType()) {
					return 0;
				} else if (refTypeParam.getInnerType() instanceof JavaArrayType actualInner
						&& refTypeFormal.getInnerType() instanceof JavaArrayType formalInner)
					return actualInner.equals(formalInner) ? 0 : -1;

				// from here on, we should suppose that the inner types are
				// units
				UnitType paramUnitType = refTypeParam.getInnerType().asUnitType();
				UnitType formalUnitType = refTypeFormal.getInnerType().asUnitType();
				if (paramUnitType != null && formalUnitType != null) {
					int paramDist = distanceBetweenCompilationUnits(formalUnitType.getUnit(), paramUnitType.getUnit());
					if (paramDist < 0) {
						return -1;
					}
					distance += paramDist;
				} else {
					return -1;
				}
			} else {
				return -1;
			}
		}
		return distance;
	}

	/**
	 * Computes the inheritance distance between two compilation units.
	 * <p>
	 * The distance is the number of steps from {@code unit1} up its hierarchy
	 * until {@code unit2} is reached. If {@code unit2} is not reachable,
	 * returns -1.
	 * </p>
	 *
	 * @param unit1 the starting compilation unit
	 * @param unit2 the target compilation unit
	 *
	 * @return the distance, or {@code -1} if no relationship exists
	 */
	private int distanceBetweenCompilationUnits(
			CompilationUnit unit1,
			CompilationUnit unit2) {
		if (unit1.equals(unit2))
			return 0;
		for (CompilationUnit ancestor : unit2.getImmediateAncestors()) {
			int dist = distanceBetweenCompilationUnits(unit1, ancestor);
			if (dist < 0)
				continue;
			return 1 + dist;
		}
		return -1;
	}
}

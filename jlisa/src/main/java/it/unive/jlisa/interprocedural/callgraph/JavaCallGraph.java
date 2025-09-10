package it.unive.jlisa.interprocedural.callgraph;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.analysis.symbols.*;
import it.unive.lisa.interprocedural.callgraph.BaseCallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class JavaCallGraph extends BaseCallGraph {

    @Override
    public void resolveNonInstance(UnresolvedCall call, Set<Type>[] types, Collection<CFG> targets, Collection<NativeCFG> natives, SymbolAliasing aliasing) throws CallResolutionException {
        CompilationUnit targetUnit = JavaClassType.lookup(call.getQualifier(), null).getUnit();

        HierarcyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
        Set<CompilationUnit> seen = new HashSet<>();
        boolean foundPerfect = false;
        boolean multipleCallTargets = false;
        int lowestDistanceSoFar = Integer.MAX_VALUE;
        Collection<CFG> cuTargets = new LinkedHashSet<CFG>();
        Collection<NativeCFG> cuNatives = new LinkedHashSet<NativeCFG>();
        for (CompilationUnit cu : strategy.traverse(call, targetUnit)) {
            if (seen.add(cu)) {
                // we inspect only the ones of the current unit
                for (CodeMember cm : cu.getCodeMembers()) {
                    if (isATarget(call, aliasing, cm, types, false)) {
                        int distance = distanceFromPerfectTarget(call, cm, false);
                        int prevlowestDistanceSoFar = lowestDistanceSoFar;
                        if (distance < lowestDistanceSoFar) {
                            lowestDistanceSoFar = distance;
                            cuTargets = new LinkedHashSet<CFG>();
                            cuNatives = new LinkedHashSet<NativeCFG>();
                            if (cm instanceof CFG cmCFG) {
                                cuTargets = new LinkedHashSet<CFG>();
                                cuTargets.add(cmCFG);
                            } else {
                                cuNatives = new LinkedHashSet<NativeCFG>();
                                cuNatives.add((NativeCFG) cm);
                            }
                            if (distance == 0) {
                                targets.addAll(cuTargets);
                                natives.addAll(cuNatives);
                                return;
                            }
                            multipleCallTargets = false;
                        }
                        if (distance == prevlowestDistanceSoFar) {
                            multipleCallTargets = true;
                        }
                    }
                }
                if (multipleCallTargets) {
                    throw new CallResolutionException(
                            "Multiple call target for call " + call + " found in " + targetUnit + ": [" + cuTargets + " " + cuNatives);
                }
            }
        }
        targets.addAll(cuTargets);
        natives.addAll(cuNatives);
        return;
    }

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
                    "An instance call should have at least one parameter to be used as the receiver of the call");
        Expression receiver = call.getParameters()[0];
        for (Type recType : getPossibleTypesOfReceiver(receiver, types[0])) {
            CompilationUnit unit = getReceiverCompilationUnit(recType);
            if (unit == null) {
                continue;
            }
            HierarcyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
            Set<CompilationUnit> seen = new HashSet<>();
            boolean foundPerfect = false;
            boolean multipleCallTargets = false;
            int lowestDistanceSoFar = Integer.MAX_VALUE;
            for (CompilationUnit cu : strategy.traverse(call, unit)) {
                if (seen.add(cu)) {
                    Collection<CFG> cuTargets = new LinkedHashSet<CFG>();
                    Collection<NativeCFG> cuNatives = new LinkedHashSet<NativeCFG>();;
                    // we inspect only the ones of the current unit
                    for (CodeMember cm : cu.getInstanceCodeMembers(false)) {
                        if (isATarget(call, aliasing, cm, types, true)) {
                            int distance = distanceFromPerfectTarget(call, cm, true);
                            int prevlowestDistanceSoFar = lowestDistanceSoFar;
                            if (distance < lowestDistanceSoFar) {
                                multipleCallTargets = false;
                                lowestDistanceSoFar = distance;
                                cuTargets = new LinkedHashSet<CFG>();
                                cuNatives = new LinkedHashSet<NativeCFG>();
                                if (cm instanceof CFG cmCFG) {
                                    cuTargets = new LinkedHashSet<CFG>();
                                    cuTargets.add(cmCFG);
                                } else {
                                    cuNatives = new LinkedHashSet<NativeCFG>();
                                    cuNatives.add((NativeCFG) cm);
                                }
                                if (distance == 0) {
                                    foundPerfect = true;
                                    if (cm instanceof CFG cmCFG) {
                                        cuTargets.add(cmCFG);
                                    } else {
                                        cuNatives.add((NativeCFG) cm);
                                    }
                                    break;
                                }
                            }
                            if (distance == prevlowestDistanceSoFar) {
                                multipleCallTargets = true;
                            }
                        }
                    }
                    if (multipleCallTargets) {
                        throw new CallResolutionException(
                                "Multiple call target for call " + call + " found in " + unit +": [" + cuTargets + " " + cuNatives + "]");
                    }
                    // there should be only one target.
                    targets.addAll(cuTargets);
                    natives.addAll(cuNatives);
                    if (foundPerfect) {
                        break;
                    }
                }
            }
        }
    }

    public CompilationUnit getReceiverCompilationUnit(Type receiverType) {
        if (receiverType.isUnitType())
            return receiverType.asUnitType().getUnit();
        else if (receiverType.isPointerType() && receiverType.asPointerType().getInnerType().isUnitType())
            return receiverType.asPointerType().getInnerType().asUnitType().getUnit();
        else
            return null;
    }

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
            Aliases nAlias = aliasing.getState(new NameSymbol(name));
            Aliases qAlias = aliasing.getState(new QualifierSymbol(qualifier));
            Aliases qnAlias = aliasing.getState(new QualifiedNameSymbol(qualifier, name));

            // we first check the qualified name, then the qualifier and the
            // name individually
            if (!qnAlias.isEmpty()) {
                for (QualifiedNameSymbol alias : qnAlias.castElements(QualifiedNameSymbol.class))
                    if (matchCodeMemberName(call, alias.getQualifier(), alias.getName())) {
                        target = true;
                        break;
                    }
            }

            if (!target && !qAlias.isEmpty()) {
                for (QualifierSymbol alias : qAlias.castElements(QualifierSymbol.class))
                    if (matchCodeMemberName(call, alias.getQualifier(), name)) {
                        target = true;
                        break;
                    }
            }

            if (!target && !nAlias.isEmpty()) {
                for (NameSymbol alias : nAlias.castElements(NameSymbol.class))
                    if (matchCodeMemberName(call, qualifier, alias.getName())) {
                        target = true;
                        break;
                    }
            }
        }

        if (!target)
            target = matchCodeMemberName(call, qualifier, name);
        ParameterMatchingStrategy strategy = call.getProgram().getFeatures().getMatchingStrategy();

        return target && strategy.matches(call, descr.getFormals(), call.getParameters(), types);
        /*ParameterMatchingStrategy strategy = call.getProgram().getFeatures().getMatchingStrategy();
        if (add && strategy.matches(call, descr.getFormals(), call.getParameters(), types))
            if (cm instanceof CFG)
                targets.add((CFG) cm);
            else
                natives.add((NativeCFG) cm);*/
    }

    private int distanceFromPerfectTarget(UnresolvedCall call, CodeMember cm, boolean instance) {
        int distance = 0;
        int index = instance ? 1 : 0;
        for (int i = index; i < call.getParameters().length; i++) {
            Expression parameter = call.getParameters()[i];
            if (parameter.getStaticType() instanceof JavaNumericType numericParameter) {
                if (cm.getDescriptor().getFormals()[i].getStaticType() instanceof JavaNumericType numericFormal) {
                    int parameterDistance = numericParameter.distance(numericFormal);
                    if (parameterDistance < 0) {
                        return -1; // incomparable.
                    }
                    distance += parameterDistance;
                } else {
                    return -1; // incomparable (
                }
            }else if (parameter.getStaticType() instanceof UnitType unitParameter) {
                if (cm.getDescriptor().getFormals()[i].getStaticType() instanceof UnitType unitFormal) {
                    int parameterDistance = distanceBetweenCompilationUnits(unitFormal.getUnit(), unitParameter.getUnit());
                    if (parameterDistance < 0) {
                        return -1; // incomparable.
                    }
                    distance += parameterDistance;
                } else {
                    return -1;
                }
            }
        }
        return distance;
    }

    private int distanceBetweenCompilationUnits(CompilationUnit unit1, CompilationUnit unit2) {
        if (unit1.equals(unit2)) {
            return 0;
        }
        for (CompilationUnit ancestorUnit : unit1.getImmediateAncestors()) {
            int innerDistance = distanceBetweenCompilationUnits(ancestorUnit, unit2);
            if (innerDistance < 0) {
                return -1;
            }
            return 1 + innerDistance;
        }
        return -1;
    }


}

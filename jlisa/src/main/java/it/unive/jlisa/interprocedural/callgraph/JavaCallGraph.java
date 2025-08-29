package it.unive.jlisa.interprocedural.callgraph;

import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.interprocedural.callgraph.BaseCallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.type.Type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class JavaCallGraph extends BaseCallGraph {

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
            CompilationUnit unit;
            if (recType.isUnitType())
                unit = recType.asUnitType().getUnit();
            else if (recType.isPointerType() && recType.asPointerType().getInnerType().isUnitType())
                unit = recType.asPointerType().getInnerType().asUnitType().getUnit();
            else
                continue;

            Set<CompilationUnit> seen = new HashSet<>();
            HierarcyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
            boolean found = false;
            for (CompilationUnit cu : strategy.traverse(call, unit)) {
                if (seen.add(cu)) {
                    // we inspect only the ones of the current unit
                    int prevTargetsSize = targets.size();
                    for (CodeMember cm : cu.getInstanceCodeMembers(false)) {
                        checkMember(call, types, targets, natives, aliasing, cm, true);
                        // if checkMember adds some targets, we stop traversing superunits: 
                        // we have found our targets
                        if (targets.size() > prevTargetsSize) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }
    }
}

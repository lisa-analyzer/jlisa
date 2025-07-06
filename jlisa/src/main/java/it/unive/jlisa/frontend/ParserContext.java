package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.JavaProgram;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ParserContext {
    public enum EXCEPTION_HANDLING_STRATEGY {
        FAIL,
        COLLECT
    }

    private JavaProgram program;
    private int apiLevel;
    private List<ParsingException> exceptions;

    private EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy;

    Map<CFG, Map<String, Type>> variableTypes = new HashMap<>();
    public ParserContext(JavaProgram program, int apiLevel, EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy) {
        this.program = program;
        this.apiLevel = apiLevel;
        this.exceptions = new ArrayList<>();
        this.exceptionHandlingStrategy = exceptionHandlingStrategy;
    }

    public void addVariableType(CFG cfg, String variableName, Type type) {
        Map<String, Type> types = variableTypes.get(cfg);
        if (types == null) {
            types = new HashMap<>();
            variableTypes.put(cfg, types);
        }
        if (types.get(variableName) != null) {
            throw new RuntimeException("Variable " + variableName + " already exists in the cfg");
        }
        types.put(variableName, type);
    }
    public Type getVariableStaticType(CFG cfg, String name) {
        Type type = null;
        Map<String, Type> cfgVariables = variableTypes.get(cfg);
        if (cfgVariables != null) {
            type = cfgVariables.get(name);
        }
        if (type == null) {
            Unit unit = cfg.getDescriptor().getUnit();
            while (unit != null) {
                if (unit instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) unit;
                    for (Global g : cu.getGlobals()) {
                        if (g.getName().equals(name)) {
                            return g.getStaticType();
                        }
                    }
                    for (Global g : cu.getInstanceGlobals(false)) {
                        if (g.getName().equals(name)) {
                            return g.getStaticType();
                        }
                    }
                    if (cu.getImmediateAncestors().isEmpty()) {
                        unit = null;
                    } else {
                        unit = cu.getImmediateAncestors().iterator().next();
                    }
                } else {
                    for (Global g : unit.getGlobals()) {
                        if (g.getName().equals(name)) {
                            return g.getStaticType();
                        }
                    }
                }

            }
            return  JavaClassType.lookup(name).isPresent() ? JavaClassType.lookup(name).get() : Untyped.INSTANCE;
        }
        return type;
    }

    public JavaProgram getProgram() {
        return program;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public List<ParsingException> getExceptions() {
        return exceptions;
    }

    public void addException(ParsingException e) {
        if (exceptionHandlingStrategy == EXCEPTION_HANDLING_STRATEGY.FAIL) {
            throw new RuntimeException(e);
        }
        exceptions.add(e);
    }
}

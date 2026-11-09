package it.unive.jlisa.springed.security.p1;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;

import java.util.ArrayList;
import java.util.List;

public class P1securityImpl implements P1security {

    private final List<String> controllerAnnotationNames = List.of(
            "Component",
            "Configuration");

    private final ParserContext parserContext;

    public P1securityImpl(ParserContext parserContext) {
        this.parserContext = parserContext;
    }

    @Override
    public SecurityChains p1(Unit[] p) {
        SecurityChains securityChains = new SecurityChains();

        this.getSecurityConfigurations(p);

        return null;
    }

    public List<ClassUnit> getSecurityConfigurations(Unit[] units) {

        List<ClassUnit> classes = new ArrayList<>();

        for (Unit unit : units) {
            if (unit instanceof ClassUnit) {
                classes.add((ClassUnit) unit);
            }
        }

        List<ClassUnit> securityConfigurations = new ArrayList<>();
        for (ClassUnit classUnit : classes) {
            String x = "1";
        }

        return null;
    }
}

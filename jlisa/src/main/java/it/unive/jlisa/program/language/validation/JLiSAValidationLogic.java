package it.unive.jlisa.program.language.validation;

import it.unive.lisa.program.*;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.language.validation.BaseValidationLogic;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class JLiSAValidationLogic extends BaseValidationLogic {

    private String STATEMENT_LOCATIONS = "Two statements at the same location %s: %s - %s";
    private Map<String, Object> locations = new HashMap<String, Object>();

    public void validateAndFinalize(
            CodeUnit unit)
            throws ProgramValidationException {
        validateLocation(unit.getLocation().getCodeLocation(), unit);
        super.validateAndFinalize(unit);
    }

    private void validateLocation(String location, Object programEntity) throws ProgramValidationException {
        if (locations.containsKey(location) && !locations.get(location).equals(programEntity)) {
            throw new ProgramValidationException(format(STATEMENT_LOCATIONS, location, locations.get(location).toString(), programEntity.toString()));
        }
        locations.put(location, programEntity);
    }
    public void validateAndFinalize(
            CompilationUnit unit)
            throws ProgramValidationException {
        validateLocation(unit.getLocation().getCodeLocation(), unit);
        super.validateAndFinalize(unit);
    }

    public void validate(
            CodeMember member,
            boolean instance)
            throws ProgramValidationException {
        validateLocation(member.getDescriptor().getLocation().getCodeLocation(), member);
        if (member instanceof CFG cfg) {
            for (Statement statement : cfg.getNodes()) {
                validateLocation(statement.getLocation().getCodeLocation(), statement);
            }
        }
        super.validate(member, instance);
    }

    public void validate(
            Global global,
            boolean isInstance)
            throws ProgramValidationException {
        validateLocation(global.getLocation().getCodeLocation(), global);
        super.validate(global, isInstance);
    }
}

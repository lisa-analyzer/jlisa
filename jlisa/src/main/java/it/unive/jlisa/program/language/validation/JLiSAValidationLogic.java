package it.unive.jlisa.program.language.validation;

import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.language.validation.BaseValidationLogic;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class JLiSAValidationLogic extends BaseValidationLogic {

    private String STATEMENT_LOCATIONS = "Two statements at the same location %s: %s - %s";
    private Map<String, Object> locations = new HashMap<String, Object>();

    @Override
    public void validateAndFinalize(
            CodeUnit unit)
            throws ProgramValidationException {
        validateLocation(unit.getLocation().getCodeLocation(), unit);
        super.validateAndFinalize(unit);
    }
    
    @Override
    public void validateAndFinalize(
            CompilationUnit unit)
            throws ProgramValidationException {
        validateLocation(unit.getLocation().getCodeLocation(), unit);
        super.validateAndFinalize(unit);
    }

    @Override
    public void validate(
            CodeMember member,
            boolean instance)
            throws ProgramValidationException {
        validateLocation(member.getDescriptor().getLocation().getCodeLocation(), member);
        if (member instanceof CFG cfg) {
            for (Statement statement : cfg.getNodes()) {
                validate(statement);
            }
        }
        super.validate(member, instance);
    }

    @Override
    public void validate(
            Global global,
            boolean isInstance)
            throws ProgramValidationException {
        validateLocation(global.getLocation().getCodeLocation(), global);
        super.validate(global, isInstance);
    }
    
    public void validate(Statement statement) throws ProgramValidationException {
        validateLocation(statement.getLocation().getCodeLocation(), statement);
        if (statement instanceof NaryExpression naryExpression) {
            for (Expression e : naryExpression.getSubExpressions()) {
                validate(e);
            }
        }
    }
        
    private void validateLocation(String location, Object programEntity) throws ProgramValidationException {
        if (locations.containsKey(location) && !locations.get(location).equals(programEntity)) {
            throw new ProgramValidationException(format(STATEMENT_LOCATIONS, location, locations.get(location).toString(), programEntity.toString()));
        }
        locations.put(location, programEntity);
    }
}

package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.lisa.program.Program;

import java.util.ArrayList;
import java.util.List;


public class ParserContext {
    public enum EXCEPTION_HANDLING_STRATEGY {
        FAIL,
        COLLECT
    }

    private Program program;
    private int apiLevel;
    private List<ParsingException> exceptions;

    private EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy;

    public ParserContext(Program program, int apiLevel, EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy) {
        this.program = program;
        this.apiLevel = apiLevel;
        this.exceptions = new ArrayList<>();
        this.exceptionHandlingStrategy = exceptionHandlingStrategy;
    }

    public Program getProgram() {
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

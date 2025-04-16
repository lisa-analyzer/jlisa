package it.unive.jlisa.frontend.exceptions;


import it.unive.lisa.program.cfg.statement.Statement;

import java.io.Serial;

public class UnsupportedStatementException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3217861037317417216L;

    public UnsupportedStatementException() {
        super();
    }

    public UnsupportedStatementException(
            String message) {
        super(message);
    }

    public UnsupportedStatementException(
            Statement s) {
        super("Not supported yet:" + s);
    }
}

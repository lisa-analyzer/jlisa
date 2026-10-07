package it.unive.jlisa.springed.exceptions;

import java.io.Serial;

public class PathMergeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3453183566688585650L;

    private final String controllerPath;
    private final String methodPath;

    public PathMergeException(String message, String controllerPath, String methodPath) {
        super(message);
        this.controllerPath = controllerPath;
        this.methodPath = methodPath;
    }

    public String getControllerPath() {
        return this.controllerPath;
    }

    public String getMethodPath() {
        return this.methodPath;
    }
}

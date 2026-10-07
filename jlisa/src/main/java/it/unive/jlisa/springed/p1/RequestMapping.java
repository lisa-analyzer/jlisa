package it.unive.jlisa.springed.p1;
import java.util.Set;

public class RequestMapping {

    private final Set<String> methods;
    private final Set<String> paths;
    private final Set<String> params;
    private final Set<String> headers;
    private final Set<String> consumes;
    private final Set<String> produces;
    private final String version;

    public RequestMapping(Set<String> methods, Set<String> paths, Set<String> params, Set<String> headers, Set<String> consumes, Set<String> produces, String version) {
        this.methods = methods;
        this.paths = paths;
        this.params = params;
        this.headers = headers;
        this.consumes = consumes;
        this.produces = produces;
        this.version = version;
    }

    public Set<String> getMethods() {
        return methods;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public Set<String> getParams() {
        return params;
    }

    public Set<String> getHeaders() {
        return headers;
    }

    public Set<String> getConsumes() {
        return consumes;
    }

    public Set<String> getProduces() {
        return produces;
    }

    public String getVersion() { return version; }
}

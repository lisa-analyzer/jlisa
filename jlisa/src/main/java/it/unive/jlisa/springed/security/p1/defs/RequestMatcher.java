package it.unive.jlisa.springed.security.p1.defs;

public class RequestMatcher {

    private final String pattern;
    private final String method;
    private final Integer order;

    public RequestMatcher(String pattern, String method, Integer order) {
        this.pattern = pattern;
        this.method = method;
        this.order = order;
    }

    public String getPattern() {
        return this.pattern;
    }

    public String getMethod() {
        return this.method;
    }

    public Integer getOrder() {
        return this.order;
    }
}

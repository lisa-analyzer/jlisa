package it.unive.jlisa.frontend.annotations;

import java.util.Collections;
import java.util.Map;

public class AnnotationInfo {
    private final String name;
    private final Map<String, String> params;  //  {"value": "/user/info"} {"path": "/user/info"}

    public AnnotationInfo(String name, Map<String, String> params) {
        this.name = name;
        this.params = params == null ? Collections.emptyMap() : Map.copyOf(params);
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "@" + name + params;
    }
}

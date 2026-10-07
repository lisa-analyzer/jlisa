package it.unive.jlisa.springed.p1.util;

import it.unive.jlisa.springed.p1.RequestMapping;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestMappingBuilder {

    public static RequestMapping build(Annotation annotation) {
        List<AnnotationMember> annotationBody = annotation.getAnnotationMembers();

        return new RequestMapping(
                getMethods(annotation.getAnnotationName(), annotationBody),
                getPaths(annotationBody),
                getParams(annotationBody),
                getHeaders(annotationBody),
                getConsumes(annotationBody),
                getProduces(annotationBody),
                getVersion(annotationBody));
    }

    private static Set<String> getMethods(String annotationName, List<AnnotationMember> annotationBody) {
        Set<String> methods = new HashSet<>();

        switch (annotationName) {
            case "RequestMapping" -> methods.addAll(getValues(annotationBody, "method"));
            case "GetMapping" -> methods.add("GET");
            case "PostMapping" -> methods.add("POST");
            case "PutMapping" -> methods.add("PUT");
            case "DeleteMapping" -> methods.add("DELETE");
            case "PatchMapping" -> methods.add("PATCH");
        }

        return methods;
    }

    private static Set<String> getPaths(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "value", "path");
    }

    private static Set<String> getParams(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "params");
    }

    private static Set<String> getHeaders(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "headers");
    }

    private static Set<String> getConsumes(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "consumes");
    }

    private static Set<String> getProduces(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "produces");
    }

    private static String getVersion(List<AnnotationMember> annotationBody) {
        return getValues(annotationBody, "version").stream().findFirst().orElse("");
    }

    private static Set<String> getValues(List<AnnotationMember> annotationBody, String... ids) {
        Set<String> values = new HashSet<>();

        for (AnnotationMember element : annotationBody) {
            if (List.of(ids).contains(element.getId())) {
                if (element.getValue() instanceof ArrayAnnotationValue arrayAnnotationValue) {
                    for (BasicAnnotationValue arrayValue : arrayAnnotationValue.getArray()) {
                        values.add(unquote(arrayValue.toString()));
                    }
                } else {
                    values.add(unquote(element.getValue().toString()));
                }
            }
        }

        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}

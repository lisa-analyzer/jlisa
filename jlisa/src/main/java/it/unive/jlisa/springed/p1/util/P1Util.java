package it.unive.jlisa.springed.p1.util;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import java.util.ArrayList;
import java.util.List;

public class P1Util {

	public static String getHttpMethod(
			Annotation annotation) {
		return switch (annotation.getAnnotationName()) {
		case "GetMapping" -> "GET";
		case "PostMapping" -> "POST";
		case "PutMapping" -> "PUT";
		case "DeleteMapping" -> "DELETE";
		case "PatchMapping" -> "PATCH";
		// @RequestMapping(method = RequestMethod.GET) -> "GET".
		// Enum-valued members are not yet parsed by AnnotationBuilder, so this
		// branch is currently unreachable.
		case "RequestMapping" -> throw new UnsupportedOperationException("Unsupported annotation variation");
		default -> null;
		};
	}

	public static String getPath(
			Annotation annotation) {
		for (AnnotationMember m : annotation.getAnnotationMembers())
			for (String name : new String[] { "value", "path" })
				if (m.getId().equals(name)) {
					return m.getValue() == null ? null : firstOrNull(toStringList(m.getValue()));
				}

		return null;
	}

	// The *AnnotationValue classes expose no getters, so values are recovered
	// via toString().
	private static List<String> toStringList(
			AnnotationValue value) {
		if (value instanceof StringAnnotationValue)
			return List.of(unquote(value.toString()));

		if (value instanceof ArrayAnnotationValue) {
			String text = value.toString();
			if (text.length() >= 2)
				text = text.substring(1, text.length() - 1);
			if (text.isBlank())
				return new ArrayList<>();
			List<String> result = new ArrayList<>();
			for (String element : text.split(", "))
				result.add(unquote(element));
			return result;
		}

		return List.of(value.toString());
	}

//    private Map<String, Object> toPairMap(AnnotationValue value) {
//        Map<String, Object> map = new HashMap<>();
//        for (String entry : toStringList(value)) {
//            int eq = entry.indexOf('=');
//            if (eq < 0) map.put(entry, "");
//            else map.put(entry.substring(0, eq), entry.substring(eq + 1));
//        }
//        return map;
//    }

	private static String unquote(
			String s) {
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
			return s.substring(1, s.length() - 1);
		return s;
	}

	private static String firstOrNull(
			List<String> list) {
		return list.isEmpty() ? null : list.get(0);
	}
}

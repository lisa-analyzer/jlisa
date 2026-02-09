package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;

/**
 * Extended Parameter class that includes annotations in toString() output.
 * This ensures parameter annotations (like @RequestParam) are displayed in HTML signatures.
 */
public class JavaParameter extends Parameter {

	/**
	 * Builds a Java parameter with annotations support.
	 * 
	 * @param location    the location where this parameter is defined
	 * @param name        the name of this parameter
	 * @param staticType  the type of this parameter
	 * @param defaultValue the default value for this parameter
	 * @param annotations the annotations of this parameter
	 */
	public JavaParameter(
			CodeLocation location,
			String name,
			Type staticType,
			Expression defaultValue,
			Annotations annotations) {
		super(location, name, staticType, defaultValue, annotations);
	}

	@Override
	public String toString() {
		// Include annotations in the string representation
		// Format: "@Annotation1 @Annotation2 Type name"
		Annotations anns = getAnnotations();
		StringBuilder sb = new StringBuilder();
		
		if (anns != null && !anns.isEmpty()) {
			String s = anns.toString().trim();
			// remove surrounding []
			if (s.startsWith("[") && s.endsWith("]"))
				s = s.substring(1, s.length() - 1).trim();
			
			if (!s.isEmpty()) {
				// "A, B" -> "@A @B"
				String[] parts = s.split("\\s*,\\s*");
				for (String p : parts) {
					if (p == null || p.isBlank())
						continue;
					String t = p.trim();
					if (!t.startsWith("@"))
						sb.append("@");
					sb.append(t.startsWith("@") ? t.substring(1) : t);
					sb.append(" ");
				}
			}
		}
		
		// Append type and name
		sb.append(getStaticType()).append(" ").append(getName());
		return sb.toString();
	}
}


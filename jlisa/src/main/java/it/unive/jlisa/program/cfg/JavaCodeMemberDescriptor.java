package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.Type;

public class JavaCodeMemberDescriptor extends CodeMemberDescriptor {
	private String graphTitlePrefix;

	public JavaCodeMemberDescriptor(
			CodeLocation location,
			Unit unit,
			boolean instance,
			String name,
			Parameter... formals) {
		super(location, unit, instance, name, formals);
	}

	public JavaCodeMemberDescriptor(
			CodeLocation location,
			Unit unit,
			boolean instance,
			String name,
			Type returnType,
			Parameter... formals) {
		super(location, unit, instance, name, returnType, formals);
	}

	public JavaCodeMemberDescriptor(
			CodeLocation location,
			Unit unit,
			boolean instance,
			String name,
			Type returnType,
			Annotations annotations,
			Parameter... formals) {
		super(location, unit, instance, name, returnType, annotations, formals);
	}

	/**
	 * Helper method to get return type from a descriptor via reflection.
	 */
	private static Type getReturnTypeFromDescriptor(CodeMemberDescriptor desc) {
		try {
			java.lang.reflect.Method getReturnTypeMethod = CodeMemberDescriptor.class.getMethod("getReturnType");
			return (Type) getReturnTypeMethod.invoke(desc);
		} catch (Exception e) {
			// Try getStaticType as fallback
			try {
				java.lang.reflect.Method getStaticTypeMethod = CodeMemberDescriptor.class.getMethod("getStaticType");
				return (Type) getStaticTypeMethod.invoke(desc);
			} catch (Exception e2) {
				// If both fail, return VoidType as default
				return it.unive.lisa.type.VoidType.INSTANCE;
			}
		}
	}

	/**
	 * Copy constructor - creates a new JavaCodeMemberDescriptor from another one.
	 * This ensures annotations and all fields are properly copied.
	 */
	public JavaCodeMemberDescriptor(
			JavaCodeMemberDescriptor other) {
		// Call super constructor first (required before any other statements)
		// Get return type via helper method
		super(other.getLocation(), other.getUnit(), other.isInstance(), other.getName(),
				getReturnTypeFromDescriptor(other), other.getAnnotations(), other.getFormals());
		// Now we can set fields
		this.graphTitlePrefix = other.graphTitlePrefix;
		// Copy overridable state - try to get it via reflection if needed
		try {
			java.lang.reflect.Method isOverridableMethod = CodeMemberDescriptor.class.getMethod("isOverridable");
			Object result = isOverridableMethod.invoke(other);
			if (result instanceof Boolean) {
				setOverridable((Boolean) result);
			}
		} catch (Exception e) {
			// Method might not exist or be accessible, ignore
		}
	}

	public void setGraphTitlePrefix(
			String prefix) {
		this.graphTitlePrefix = prefix;
	}

	public String getGraphTitlePrefix() {
		return graphTitlePrefix;
	}

	@Override
	public String getSignature() {
		// Base textual signature + annotations stored in this descriptor
		String base = super.getSignature();
		return signatureWithAnnotations(base);
	}

	@Override
	public String getFullSignature() {
		String base = super.getFullSignature();
		return signatureWithAnnotations(base);
	}

	@Override
	public String getFullSignatureWithParNames() {
		// CRITICAL: LiSA uses getFullSignatureWithParNames() in SerializableCFG.fromCFG()
		// for HTML titles and DOT graph names. Just decorate the base representation
		// with the annotations already attached to this descriptor.
		String base = super.getFullSignatureWithParNames();
		return signatureWithAnnotations(base);
	}

	@Override
	public String toString() {
		String base = super.toString();
		return signatureWithAnnotations(base);
	}

	private String signatureWithAnnotations(
			String base) {
		// Use annotations from Annotations object - this is what LiSA uses for HTML
		// This method is similar to what ModuleDotGraph should do
		Annotations anns = getAnnotations();
		if (anns == null || anns.isEmpty())
			return base;

		String s = anns.toString().trim();
		// remove surrounding []
		if (s.startsWith("[") && s.endsWith("]"))
			s = s.substring(1, s.length() - 1).trim();

		if (s.isEmpty())
			return base;

		// remove the trailing [] that LiSA prints for annotations
		s = s.replace("[]", "").trim();

		if (s.isEmpty())
			return base;

		// "A, B" -> "@A @B"
		String[] parts = s.split("\\s*,\\s*");
		StringBuilder pref = new StringBuilder();
		for (String p : parts) {
			if (p == null || p.isBlank())
				continue;
			if (pref.length() > 0)
				pref.append(" ");
			String t = p.trim();
			if (!t.startsWith("@"))
				pref.append("@");
			pref.append(t.startsWith("@") ? t.substring(1) : t);
		}

		if (pref.length() == 0)
			return base;

		return pref + " " + base;
	}

	public boolean matchesSignature(
			CodeMemberDescriptor reference) {
		if (!getName().equals(reference.getName()))
			return false;
		if (getFormals().length != reference.getFormals().length)
			return false;
		for (int i = 0; i < getFormals().length; i++) {
			if (!getFormals()[i].getStaticType().equals(reference.getFormals()[i].getStaticType())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Override clone() to ensure JavaCodeMemberDescriptor is returned and annotations are preserved.
	 * This is called if CFG or other LiSA components try to clone the descriptor.
	 */
	@Override
	public JavaCodeMemberDescriptor clone() {
		try {
			// Get return type via reflection
			Type returnType = null;
			try {
				java.lang.reflect.Method getReturnTypeMethod = CodeMemberDescriptor.class.getMethod("getReturnType");
				returnType = (Type) getReturnTypeMethod.invoke(this);
			} catch (Exception e) {
				try {
					java.lang.reflect.Method getStaticTypeMethod = CodeMemberDescriptor.class.getMethod("getStaticType");
					returnType = (Type) getStaticTypeMethod.invoke(this);
				} catch (Exception e2) {
					returnType = it.unive.lisa.type.VoidType.INSTANCE;
				}
			}
			JavaCodeMemberDescriptor cloned = new JavaCodeMemberDescriptor(
					getLocation(), getUnit(), isInstance(), getName(),
					returnType, getAnnotations(), getFormals());
			cloned.graphTitlePrefix = this.graphTitlePrefix;
			// Copy overridable state via reflection
			try {
				java.lang.reflect.Method isOverridableMethod = CodeMemberDescriptor.class.getMethod("isOverridable");
				Object result = isOverridableMethod.invoke(this);
				if (result instanceof Boolean) {
					cloned.setOverridable((Boolean) result);
				}
			} catch (Exception e) {
				// Method might not exist or be accessible, ignore
			}
			return cloned;
		} catch (Exception e) {
			throw new RuntimeException("Failed to clone JavaCodeMemberDescriptor", e);
		}
	}

	/**
	 * Override copy() if it exists in CodeMemberDescriptor.
	 * This ensures annotations are preserved when copying.
	 */
	public JavaCodeMemberDescriptor copy() {
		// Get return type via reflection
		Type returnType = null;
		try {
			java.lang.reflect.Method getReturnTypeMethod = CodeMemberDescriptor.class.getMethod("getReturnType");
			returnType = (Type) getReturnTypeMethod.invoke(this);
		} catch (Exception e) {
			try {
				java.lang.reflect.Method getStaticTypeMethod = CodeMemberDescriptor.class.getMethod("getStaticType");
				returnType = (Type) getStaticTypeMethod.invoke(this);
			} catch (Exception e2) {
				returnType = it.unive.lisa.type.VoidType.INSTANCE;
			}
		}
		JavaCodeMemberDescriptor copied = new JavaCodeMemberDescriptor(
				getLocation(), getUnit(), isInstance(), getName(),
				returnType, getAnnotations(), getFormals());
		copied.graphTitlePrefix = this.graphTitlePrefix;
		// Copy overridable state via reflection
		try {
			java.lang.reflect.Method isOverridableMethod = CodeMemberDescriptor.class.getMethod("isOverridable");
			Object result = isOverridableMethod.invoke(this);
			if (result instanceof Boolean) {
				copied.setOverridable((Boolean) result);
			}
		} catch (Exception e) {
			// Method might not exist or be accessible, ignore
		}
		return copied;
	}
}

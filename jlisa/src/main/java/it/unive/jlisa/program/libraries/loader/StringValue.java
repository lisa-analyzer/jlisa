package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import java.util.Objects;

public class StringValue implements Value {
	private final String value;

	public StringValue(
			String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringValue other = (StringValue) obj;
		return value == other.value;
	}

	@Override
	public String toString() {
		return "StringValue [value=" + value + "]";
	}

	@Override
	public Expression toLiSAExpression(
			CFG init) {
		return new StringLiteral(init, init.getDescriptor().getLocation(), clean(this.value));
	}

	private String clean(
			String text) {
		return text.substring(1, text.length() - 1);
	}
}

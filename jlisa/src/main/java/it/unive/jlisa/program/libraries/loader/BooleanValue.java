package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import java.util.Objects;

public class BooleanValue implements Value {
	private final boolean value;

	public BooleanValue(
			boolean value) {
		this.value = value;
	}

	public boolean getValue() {
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
		BooleanValue other = (BooleanValue) obj;
		return value == other.value;
	}

	@Override
	public String toString() {
		return "BooleanValue [value=" + value + "]";
	}

	@Override
	public Expression toLiSAExpression(
			CFG init) {
		if (value)
			return new TrueLiteral(init, init.getDescriptor().getLocation());
		else
			return new FalseLiteral(init, init.getDescriptor().getLocation());
	}
}

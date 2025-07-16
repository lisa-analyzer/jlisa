package it.unive.jlisa.program.libraries.loader;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import java.util.Objects;

public class NumberValue implements Value {
	private final int value;

	public NumberValue(
			int value) {
		this.value = value;
	}

	public int getValue() {
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
		NumberValue other = (NumberValue) obj;
		return value == other.value;
	}

	@Override
	public String toString() {
		return "NumberValue [value=" + value + "]";
	}

	@Override
	public Expression toLiSAExpression(
			CFG init) {
		return new Int32Literal(init, init.getDescriptor().getLocation(), value);
	}
}

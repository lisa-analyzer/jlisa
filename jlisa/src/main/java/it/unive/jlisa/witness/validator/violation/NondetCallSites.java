package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code Verifier.nondet*()} call sites of a method, read from its bytecode
 * through the debugger: tells which of the calls of a line a call is, as
 * {@link WitnessLocations} does when it writes a witness (the k-th call of the
 * line in bytecode order is the k-th in source order).
 */
final class NondetCallSites {

	private final Map<Method, List<Long>> sites = new HashMap<>();
	private final Map<ReferenceType, String[]> pools = new HashMap<>();

	/**
	 * Returns the rank, from 0, of the call at the given location among the
	 * nondet call sites of its line, with the number of those sites; null if
	 * the bytecode cannot be read.
	 */
	int[] rank(
			Location caller) {
		Method method = caller.method();
		List<Long> all = sites.computeIfAbsent(method, this::scan);
		if (all == null) {
			return null;
		}
		List<Long> onLine = new ArrayList<>();
		for (long index : all) {
			if (lineOf(method, index) == caller.lineNumber()) {
				onLine.add(index);
			}
		}
		int rank = onLine.indexOf(caller.codeIndex());
		return rank < 0 ? null : new int[] { rank, onLine.size() };
	}

	private static int lineOf(
			Method method,
			long index) {
		try {
			int line = -1;
			long best = -1;
			for (Location l : method.allLineLocations()) {
				if (l.codeIndex() <= index && l.codeIndex() > best) {
					best = l.codeIndex();
					line = l.lineNumber();
				}
			}
			return line;
		} catch (AbsentInformationException e) {
			return -1;
		}
	}

	/** The code indexes of the nondet calls of the method, or null. */
	private List<Long> scan(
			Method method) {
		try {
			if (!method.virtualMachine().canGetBytecodes() || !method.virtualMachine().canGetConstantPool()) {
				return null;
			}
			String[] pool = pools.computeIfAbsent(method.declaringType(), NondetCallSites::methodRefs);
			if (pool == null) {
				return null;
			}
			byte[] code = method.bytecodes();
			List<Long> result = new ArrayList<>();
			int pc = 0;
			while (pc < code.length) {
				int op = code[pc] & 0xff;
				if (op == 0xb6 || op == 0xb7 || op == 0xb8 || op == 0xb9) {
					int ref = ((code[pc + 1] & 0xff) << 8) | (code[pc + 2] & 0xff);
					String target = ref < pool.length ? pool[ref] : null;
					if (target != null && isNondet(target)) {
						result.add((long) pc);
					}
				}
				int length = length(code, pc);
				if (length <= 0) {
					return null;
				}
				pc += length;
			}
			return result;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static boolean isNondet(
			String target) {
		int dot = target.lastIndexOf('.');
		String owner = target.substring(0, dot);
		return target.substring(dot + 1).startsWith("nondet")
				&& (owner.equals("Verifier") || owner.endsWith("/Verifier"));
	}

	/**
	 * Returns, for each constant pool index, {@code owner.name} of the method
	 * reference there (null elsewhere), or null if the pool cannot be read.
	 */
	private static String[] methodRefs(
			ReferenceType type) {
		int count = type.constantPoolCount();
		byte[] bytes = type.constantPool();
		String[] utf8 = new String[count];
		int[] first = new int[count];
		int[] second = new int[count];
		int[] tags = new int[count];
		int at = 0;
		for (int i = 1; i < count; i++) {
			int tag = bytes[at] & 0xff;
			tags[i] = tag;
			at++;
			switch (tag) {
			case 1 -> {
				int len = u2(bytes, at);
				utf8[i] = new String(bytes, at + 2, len, StandardCharsets.UTF_8);
				at += 2 + len;
			}
			case 3, 4 -> at += 4;
			case 5, 6 -> {
				at += 8;
				i++;
			}
			case 7, 8, 16, 19, 20 -> {
				first[i] = u2(bytes, at);
				at += 2;
			}
			case 9, 10, 11, 12, 17, 18 -> {
				first[i] = u2(bytes, at);
				second[i] = u2(bytes, at + 2);
				at += 4;
			}
			case 15 -> at += 3;
			default -> {
				return null;
			}
			}
		}
		String[] refs = new String[count];
		for (int i = 1; i < count; i++) {
			if (tags[i] == 10 || tags[i] == 11) {
				int cls = first[i];
				int nat = second[i];
				if (tags[cls] == 7 && tags[nat] == 12 && utf8[first[cls]] != null && utf8[first[nat]] != null) {
					refs[i] = utf8[first[cls]] + "." + utf8[first[nat]];
				}
			}
		}
		return refs;
	}

	private static int u2(
			byte[] b,
			int at) {
		return ((b[at] & 0xff) << 8) | (b[at + 1] & 0xff);
	}

	private static int s4(
			byte[] b,
			int at) {
		return ((b[at] & 0xff) << 24) | ((b[at + 1] & 0xff) << 16) | ((b[at + 2] & 0xff) << 8) | (b[at + 3] & 0xff);
	}

	/** The length of the instruction at {@code pc}, or 0 if unknown. */
	private static int length(
			byte[] code,
			int pc) {
		int op = code[pc] & 0xff;
		if (op <= 0x0f) {
			return 1;
		}
		if (op == 0x10 || op == 0x12 || (op >= 0x15 && op <= 0x19) || (op >= 0x36 && op <= 0x3a) || op == 0xa9
				|| op == 0xbc) {
			return 2;
		}
		if (op == 0x11 || op == 0x13 || op == 0x14 || op == 0x84 || (op >= 0x99 && op <= 0xa8)
				|| (op >= 0xb2 && op <= 0xb8) || op == 0xbb || op == 0xbd || op == 0xc0 || op == 0xc1 || op == 0xc6
				|| op == 0xc7) {
			return 3;
		}
		if (op == 0xb9 || op == 0xba || op == 0xc8 || op == 0xc9) {
			return 5;
		}
		if (op == 0xc5) {
			return 4;
		}
		if (op == 0xc4) {
			return (code[pc + 1] & 0xff) == 0x84 ? 6 : 4;
		}
		if (op == 0xaa) {
			int at = (pc + 4) & ~3;
			int low = s4(code, at + 4);
			int high = s4(code, at + 8);
			return at + 12 + (high - low + 1) * 4 - pc;
		}
		if (op == 0xab) {
			int at = (pc + 4) & ~3;
			int pairs = s4(code, at + 4);
			return at + 8 + pairs * 8 - pc;
		}
		if ((op >= 0x1a && op <= 0x35) || (op >= 0x3b && op <= 0x83) || (op >= 0x85 && op <= 0x98)
				|| (op >= 0xac && op <= 0xb1) || (op >= 0xbe && op <= 0xbf) || op == 0xc2 || op == 0xc3) {
			return 1;
		}
		return 0;
	}
}

package it.unive.jlisa.witness.validator.violation;

import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.search.Trace;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * The places of a witness in the program text: the {@code )} of each
 * {@code Verifier.nondet*()} call and the target of the violation. The debugger
 * only gives lines and bytecode indexes; the columns come from the source:
 * <ul>
 * <li>a call is the k-th {@code Verifier.nondet*()} invocation of its line, in
 * source order, where k is the rank of its bytecode index among those of the
 * calls of the run on that line;</li>
 * <li>the target of an assertion is the {@code assert} statement containing the
 * line where the {@code AssertionError} is thrown;</li>
 * <li>the target of an exception is the innermost full expression containing
 * the line of the innermost frame in the task sources ({@code throw}: the
 * statement itself).</li>
 * </ul>
 * <p>
 * When a line is ambiguous (more candidates than the bytecode can tell apart),
 * there is no location: no witness is better than a wrong one. Columns start
 * from 1.
 */
public final class WitnessLocations {

	/**
	 * A place in a source file.
	 *
	 * @param sourcePath the path of the file from the package root (e.g.
	 *                       {@code Main.java})
	 * @param line       the line, from 1
	 * @param column     the column, from 1
	 */
	public record Point(
			String sourcePath,
			int line,
			int column) {
	}

	private final Collection<File> sourceDirs;
	private final Map<String, CompilationUnit> units = new HashMap<>();
	private final Map<String, Boolean> missing = new HashMap<>();

	/**
	 * @param sourceDirs the task's source directories, where the files of the
	 *                       frames are looked for
	 */
	public WitnessLocations(
			Collection<File> sourceDirs) {
		this.sourceDirs = sourceDirs.stream().filter(java.util.Objects::nonNull).toList();
	}

	/**
	 * The {@code )} of the given call of the trace, or null if it cannot be
	 * told.
	 */
	public Point call(
			Trace trace,
			Trace.Call call) {
		Trace.Frame at = call.caller();
		CompilationUnit unit = unit(at.sourcePath());
		if (unit == null || at.line() < 1) {
			return null;
		}
		List<MethodInvocation> candidates = new ArrayList<>();
		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(
					MethodInvocation node) {
				if (isNondet(node) && unit.getLineNumber(node.getName().getStartPosition()) == at.line()) {
					candidates.add(node);
				}
				return true;
			}
		});
		candidates.sort((
				a,
				b) -> Integer.compare(a.getStartPosition(), b.getStartPosition()));
		TreeSet<Long> indexes = new TreeSet<>();
		for (Trace.Call c : trace.calls()) {
			Trace.Frame f = c.caller();
			if (at.sourcePath().equals(f.sourcePath()) && f.line() == at.line()
					&& at.className().equals(f.className()) && at.methodName().equals(f.methodName())) {
				indexes.add(f.codeIndex());
			}
		}
		if (candidates.size() != indexes.size()) {
			ValidatorLogger.debug("No location for the call at {}:{}: {} invocations in the source, {} in the run",
					at.sourcePath(), at.line(), candidates.size(), indexes.size());
			return null;
		}
		MethodInvocation node = candidates.get(indexes.headSet(at.codeIndex()).size());
		return point(unit, at.sourcePath(), node.getStartPosition() + node.getLength() - 1);
	}

	/**
	 * The target of the violation of the trace, or null if it cannot be told.
	 */
	public Point target(
			Trace trace,
			Property property) {
		for (Trace.Frame f : trace.frames()) {
			CompilationUnit unit = unit(f.sourcePath());
			if (unit == null || f.line() < 1) {
				continue; // a frame of the JDK
			}
			List<ASTNode> candidates = property == Property.ASSERT
					? asserts(unit, f.line())
					: List.of();
			if (candidates.isEmpty()) {
				candidates = fullExpressions(unit, f.line());
			}
			if (candidates.size() != 1) {
				ValidatorLogger.debug("No target at {}:{}: {} candidates", f.sourcePath(), f.line(), candidates.size());
				return null;
			}
			return point(unit, f.sourcePath(), candidates.getFirst().getStartPosition());
		}
		return null;
	}

	private static List<ASTNode> asserts(
			CompilationUnit unit,
			int line) {
		List<ASTNode> found = new ArrayList<>();
		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(
					AssertStatement node) {
				if (contains(unit, node, line)) {
					found.add(node);
				}
				return true;
			}
		});
		return innermost(found);
	}

	/**
	 * The full expressions containing the line (the innermost ones, when nested
	 * in a lambda or an anonymous class), and the {@code throw} statements,
	 * which raise their exception themselves.
	 */
	private static List<ASTNode> fullExpressions(
			CompilationUnit unit,
			int line) {
		List<ASTNode> found = new ArrayList<>();
		ASTVisitor visitor = new ASTVisitor() {
			private void add(
					Expression e) {
				if (e != null && contains(unit, e, line)) {
					found.add(e);
				}
			}

			@Override
			public boolean visit(
					ExpressionStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					VariableDeclarationFragment node) {
				add(node.getInitializer());
				return true;
			}

			@Override
			public boolean visit(
					ReturnStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					ThrowStatement node) {
				if (contains(unit, node, line)) {
					found.add(node);
				}
				return true;
			}

			@Override
			public boolean visit(
					IfStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					WhileStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					DoStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					ForStatement node) {
				for (Object e : node.initializers()) {
					add((Expression) e);
				}
				add(node.getExpression());
				for (Object e : node.updaters()) {
					add((Expression) e);
				}
				return true;
			}

			@Override
			public boolean visit(
					EnhancedForStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					SwitchStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					SynchronizedStatement node) {
				add(node.getExpression());
				return true;
			}

			@Override
			public boolean visit(
					AssertStatement node) {
				add(node.getExpression());
				add(node.getMessage());
				return true;
			}
		};
		unit.accept(visitor);
		return innermost(found);
	}

	/** The nodes that contain no other node of the list. */
	private static List<ASTNode> innermost(
			List<ASTNode> nodes) {
		List<ASTNode> result = new ArrayList<>();
		for (ASTNode n : nodes) {
			boolean outer = false;
			for (ASTNode m : nodes) {
				if (m != n && m.getStartPosition() >= n.getStartPosition()
						&& m.getStartPosition() + m.getLength() <= n.getStartPosition() + n.getLength()) {
					outer = true;
					break;
				}
			}
			if (!outer) {
				result.add(n);
			}
		}
		return result;
	}

	private static boolean contains(
			CompilationUnit unit,
			ASTNode node,
			int line) {
		int start = unit.getLineNumber(node.getStartPosition());
		int end = unit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
		return start <= line && line <= end;
	}

	private static boolean isNondet(
			MethodInvocation node) {
		Expression receiver = node.getExpression();
		return node.getName().getIdentifier().startsWith("nondet") && receiver != null
				&& (receiver.toString().equals("Verifier")
						|| receiver.toString().equals("org.sosy_lab.sv_benchmarks.Verifier"));
	}

	private static Point point(
			CompilationUnit unit,
			String sourcePath,
			int position) {
		return new Point(sourcePath, unit.getLineNumber(position), unit.getColumnNumber(position) + 1);
	}

	/**
	 * The parsed source file with the given path from the package root, or null
	 * if it is not a task file.
	 */
	private CompilationUnit unit(
			String sourcePath) {
		if (sourcePath == null || missing.containsKey(sourcePath)) {
			return null;
		}
		CompilationUnit unit = units.get(sourcePath);
		if (unit != null) {
			return unit;
		}
		for (File dir : sourceDirs) {
			File file = new File(dir, sourcePath);
			if (file.isFile()) {
				try {
					unit = parse(Files.readString(file.toPath(), StandardCharsets.ISO_8859_1));
				} catch (IOException e) {
					break;
				}
				units.put(sourcePath, unit);
				return unit;
			}
		}
		missing.put(sourcePath, true);
		return null;
	}

	private static CompilationUnit parse(
			String source) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.latestSupportedJavaVersion(), options);
		parser.setCompilerOptions(options);
		parser.setSource(source.toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}
}

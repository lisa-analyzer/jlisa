package it.unive.jlisa.program.java.constructs.file;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.PushAny;

/**
 * The method {@code listFiles()} of {@code java.io.File}: it returns
 * {@code null} if the file is not a directory or if an I/O error occurs (no
 * exception is thrown), and an array of unknown files otherwise. It never
 * throws {@code SecurityException}: we assume that no security manager is set
 * in the JVM.
 */
public class FileListFiles extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public FileListFiles(
			CFG cfg,
			CodeLocation location,
			Expression file) {
		super(cfg, location, "listFiles", file);
	}

	public static FileListFiles build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FileListFiles(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaReferenceType files = new JavaReferenceType(
				JavaArrayType.lookup(JavaClassType.lookup("java.io.File").getReference(), 1));
		AnalysisState<A> none = analysis.smallStepSemantics(state, new NullConstant(getLocation()), context,
				originating);
		AnalysisState<A> some = analysis.smallStepSemantics(state, new PushAny(files, getLocation()), context,
				originating);
		return none.lub(some);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

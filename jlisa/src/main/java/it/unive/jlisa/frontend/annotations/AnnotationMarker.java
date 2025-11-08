package it.unive.jlisa.frontend.annotations;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class AnnotationMarker extends Statement {

	private final AnnotationInfo info;

	public AnnotationMarker(
			CFG cfg,
			CodeLocation location,
			AnnotationInfo info) {
		super(cfg, location);
		this.info = info;
	}

	public AnnotationInfo getInfo() {
		return info;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interproc,
			StatementStore<A> store)
			throws SemanticException {
		return entryState;
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	protected int compareSameClass(
			Statement o) {
		if (o == this)
			return 0;
		AnnotationMarker other = (AnnotationMarker) o;
		int c = this.info.getName().compareTo(other.info.getName());
		if (c != 0)
			return c;
		return String.valueOf(this.info.getParams())
				.compareTo(String.valueOf(other.info.getParams()));
	}

	@Override
	public String toString() {
		return "@@" + info.getName() + info.getParams();
	}
}

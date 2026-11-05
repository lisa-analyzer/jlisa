package it.unive.jlisa.svcomp;

import it.unive.jlisa.triage.JavaExpressionEncoder;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.triage.FirstViolationVerifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link WarningVerifier} used for SV-COMP tasks. Each task has a single
 * property, either the validity of assertions or the absence of uncaught
 * runtime exceptions, and a violation witness is needed for the property of
 * the task. Hence, the warnings about failing assertions and the ones about
 * uncaught runtime exceptions (that is, the ones reporting an
 * {@link ErrorViolation}) are verified separately, each with a
 * {@link FirstViolationVerifier}: the verification of each kind of warnings
 * stops at the first one that is confirmed, independently of the other kind.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class SVCOMPVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		WarningVerifier<A, D> {

	private final WarningVerifier<A, D> verifier;

	/**
	 * The default time, in milliseconds from the creation of the verifier
	 * (that happens, for SV-COMP tasks, after parsing the program and before
	 * analyzing it), by which the verification must end: the analyses of
	 * SV-COMP tasks are stopped after 300 seconds, and the time left is needed
	 * to start the analysis and to produce the results.
	 */
	public static final long DEFAULT_BUDGET = 230_000;

	private long deadline = System.currentTimeMillis() + DEFAULT_BUDGET;

	/**
	 * Builds the verifier, verifying each kind of warnings with a
	 * {@link FirstViolationVerifier} with default parameters, encoding the
	 * operators of the Java frontend through a {@link JavaExpressionEncoder}.
	 */
	public SVCOMPVerifier() {
		this(new FirstViolationVerifier<>(JavaExpressionEncoder::new));
	}

	/**
	 * Builds the verifier.
	 *
	 * @param verifier the verifier used for each kind of warnings
	 */
	public SVCOMPVerifier(
			WarningVerifier<A, D> verifier) {
		this.verifier = verifier;
	}

	/**
	 * Sets the time by which the verification must end (by default,
	 * {@link #DEFAULT_BUDGET} milliseconds after the creation of this
	 * verifier). Warnings that are not verified by then are left possible.
	 *
	 * @param deadline the time, as given by {@link System#currentTimeMillis()}
	 */
	@Override
	public void setDeadline(
			long deadline) {
		this.deadline = deadline;
	}

	@Override
	public Map<StatementMessage, VerificationResult> verify(
			SemanticTool<A, D> tool,
			Collection<StatementMessage> warnings)
			throws SemanticException {
		List<StatementMessage> asserts = new ArrayList<>();
		List<StatementMessage> exceptions = new ArrayList<>();
		for (StatementMessage warning : warnings)
			if (warning.getViolation() instanceof ErrorViolation)
				exceptions.add(warning);
			else
				asserts.add(warning);

		// each kind of warnings gets half of the time left, and the one left
		// by the other kind: uncaught exceptions are verified first, since
		// all their warnings must be refuted to prove their property, while
		// assertions only need a confirmed warning to disprove theirs
		Map<StatementMessage, VerificationResult> results = new LinkedHashMap<>();
		if (!exceptions.isEmpty()) {
			long now = System.currentTimeMillis();
			verifier.setDeadline(asserts.isEmpty() ? deadline : now + Math.max(0, deadline - now) / 2);
			results.putAll(verifier.verify(tool, exceptions));
		}
		if (!asserts.isEmpty()) {
			verifier.setDeadline(deadline);
			results.putAll(verifier.verify(tool, asserts));
		}
		return results;
	}
}

package it.unive.jlisa.svcomp;

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
	 * Builds the verifier, verifying each kind of warnings with a
	 * {@link FirstViolationVerifier} with default parameters.
	 */
	public SVCOMPVerifier() {
		this(new FirstViolationVerifier<>());
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

		Map<StatementMessage, VerificationResult> results = new LinkedHashMap<>();
		if (!asserts.isEmpty())
			results.putAll(verifier.verify(tool, asserts));
		if (!exceptions.isEmpty())
			results.putAll(verifier.verify(tool, exceptions));
		return results;
	}
}

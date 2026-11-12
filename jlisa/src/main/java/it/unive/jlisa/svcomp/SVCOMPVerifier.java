package it.unive.jlisa.svcomp;

import it.unive.jlisa.triage.JavaExpressionEncoder;
import it.unive.jlisa.triage.JavaGlobalWrites;
import it.unive.jlisa.triage.JavaHeapWrites;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.triage.FirstViolationVerifier;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The {@link WarningVerifier} used for SV-COMP tasks. Each task has a single
 * property, either the validity of assertions or the absence of uncaught
 * runtime exceptions, and a violation witness is needed for the property of the
 * task. Hence, the warnings about failing assertions and the ones about
 * uncaught runtime exceptions (that is, the ones reporting an
 * {@link ErrorViolation}) are verified separately, each with a
 * {@link FirstViolationVerifier}: the verification of each kind of warnings
 * stops at the first one that is confirmed, independently of the other kind.
 * If none of a kind is confirmed, the candidate confirmations of the warnings
 * that stayed possible (see {@link VerificationResult#getCandidate()}) are
 * certified, in order, with the certifier of the verifier, if any (see
 * {@link #setCertifier(Predicate)}), until one is certified.
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

	private Predicate<StatementMessage> certifier;

	/**
	 * Builds the verifier, verifying each kind of warnings with the verifier of
	 * {@link #firstViolation()}.
	 */
	public SVCOMPVerifier() {
		this(firstViolation());
	}

	/**
	 * Yields a {@link FirstViolationVerifier} with default parameters, whose
	 * {@link PdrVerifier} encodes the operators of the Java frontend through a
	 * {@link JavaExpressionEncoder}, and knows the static fields and the
	 * contents of the heap written by the methods of the program
	 * ({@link JavaGlobalWrites} and {@link JavaHeapWrites}).
	 *
	 * @param <A> the kind of {@link AbstractLattice} produced by the domain
	 *                {@code D}
	 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
	 *
	 * @return the verifier
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> FirstViolationVerifier<A,
			D> firstViolation() {
		return new FirstViolationVerifier<>(new ReachabilityVerifier<>(),
				new PdrVerifier<A, D>().withFixpointCheck(false).withEncoder(JavaExpressionEncoder::new)
						.withGlobalWrites(new JavaGlobalWrites()).withHeapWrites(new JavaHeapWrites()));
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
			results.putAll(certify(asserts, verifier.verify(tool, asserts)));
		if (!exceptions.isEmpty())
			results.putAll(certify(exceptions, verifier.verify(tool, exceptions)));
		return results;
	}

	/**
	 * Sets the certifier of the candidate confirmations of the warnings that
	 * stay possible: given the copy of a warning confirmed with a candidate,
	 * it tells whether the candidate is certified (e.g., by executing the
	 * program with its reproducing inputs).
	 *
	 * @param certifier the certifier, or {@code null} for none
	 */
	public void setCertifier(
			Predicate<StatementMessage> certifier) {
		this.certifier = certifier;
	}

	/**
	 * Certifies the candidate confirmations of the given warnings, in order,
	 * if none of them has been confirmed, until one is certified, that is then
	 * confirmed.
	 */
	private Map<StatementMessage, VerificationResult> certify(
			List<StatementMessage> warnings,
			Map<StatementMessage, VerificationResult> results) {
		if (certifier == null || results.values().stream().anyMatch(VerificationResult::isConfirmed))
			return results;
		for (StatementMessage warning : warnings) {
			VerificationResult result = results.get(warning);
			if (result == null || !result.isPossible() || result.getCandidate() == null)
				continue;
			VerificationResult candidate = result.getCandidate();
			if (certifier.test(warning.verified(candidate))) {
				Map<StatementMessage, VerificationResult> certified = new LinkedHashMap<>(results);
				certified.put(warning, candidate);
				return certified;
			}
		}
		return results;
	}
}

package it.unive.jlisa.witness.validator.model;

import java.util.Collections;
import java.util.List;

/**
 * The complete execution plan extracted from a violation witness.
 * <p>
 * The validator uses this plan to steer the target JVM towards the violation:
 * <ol>
 * <li>{@link Interception}s — {@code Verifier.nondet*()} return values,
 * injected via {@link com.sun.jdi.ThreadReference#forceEarlyReturn}</li>
 * <li>{@link BranchDecision}s — branch directions, enforced via
 * {@link com.sun.jdi.StackFrame#setValue} at breakpoints (v2 only)</li>
 * <li>{@link AvoidPoint}s — locations whose execution signals a spurious
 * witness</li>
 * <li>{@link TargetPoint}s — locations whose execution confirms a correct
 * witness</li>
 * </ol>
 */
public final class ViolationPlan {

	private final List<Interception> interceptions;
	private final List<BranchDecision> branchDecisions;
	private final List<AvoidPoint> avoidPoints;
	private final List<TargetPoint> targetPoints;
	private final int skippedSteps;

	public ViolationPlan(
			List<Interception> interceptions,
			List<BranchDecision> branchDecisions,
			List<AvoidPoint> avoidPoints,
			List<TargetPoint> targetPoints) {
		this(interceptions, branchDecisions, avoidPoints, targetPoints, 0);
	}

	/**
	 * @param skippedSteps witness steps carrying a value that the parser could
	 *                         not use (unreadable scope, no value extracted): a
	 *                         run without them does not follow the witness
	 */
	public ViolationPlan(
			List<Interception> interceptions,
			List<BranchDecision> branchDecisions,
			List<AvoidPoint> avoidPoints,
			List<TargetPoint> targetPoints,
			int skippedSteps) {
		this.interceptions = Collections.unmodifiableList(interceptions);
		this.branchDecisions = Collections.unmodifiableList(branchDecisions);
		this.avoidPoints = Collections.unmodifiableList(avoidPoints);
		this.targetPoints = Collections.unmodifiableList(targetPoints);
		this.skippedSteps = skippedSteps;
	}

	public List<Interception> interceptions() {
		return interceptions;
	}

	public List<BranchDecision> branchDecisions() {
		return branchDecisions;
	}

	public List<AvoidPoint> avoidPoints() {
		return avoidPoints;
	}

	public List<TargetPoint> targetPoints() {
		return targetPoints;
	}

	/**
	 * Witness steps with a value that could not be used; see the constructor.
	 */
	public int skippedSteps() {
		return skippedSteps;
	}

	public boolean isEmpty() {
		return interceptions.isEmpty() && branchDecisions.isEmpty()
				&& avoidPoints.isEmpty() && targetPoints.isEmpty();
	}

	@Override
	public String toString() {
		return "ViolationPlan{interceptions=" + interceptions.size()
				+ ", branches=" + branchDecisions.size()
				+ ", avoids=" + avoidPoints.size()
				+ ", targets=" + targetPoints.size()
				+ ", skippedSteps=" + skippedSteps + "}";
	}
}

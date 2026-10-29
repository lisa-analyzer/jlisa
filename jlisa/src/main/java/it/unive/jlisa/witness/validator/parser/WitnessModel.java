package it.unive.jlisa.witness.validator.parser;

import it.unive.jlisa.witness.validator.model.CorrectnessInvariant;
import it.unive.jlisa.witness.validator.model.ViolationPlan;

import java.util.Collections;
import java.util.List;

/**
 * Unified internal representation of a parsed witness, independent of format version.
 *
 * <p>A witness is either a <em>violation witness</em> (carries a {@link ViolationPlan}) or a
 * <em>correctness witness</em> (carries a list of {@link CorrectnessInvariant}s).  The
 * appropriate validator is selected by inspecting {@link #type()}.
 */
public final class WitnessModel {

    /**
     * Broad witness category.
     */
    public enum WitnessType {
        VIOLATION, CORRECTNESS
    }

    /**
     * Source format from which this model was parsed.
     */
    public enum WitnessFormat {
        V1_GRAPHML, V2_YAML
    }

    private final WitnessType type;
    private final WitnessFormat format;

    // violation
    private final ViolationPlan violationPlan;

    // correctness
    private final List<CorrectnessInvariant> invariants;

    // metadata (informational only — not used for steering decisions)
    private final String producer;
    private final String programFile;

    private WitnessModel(
            WitnessType type,
            WitnessFormat format,
            ViolationPlan violationPlan,
            List<CorrectnessInvariant> invariants,
            String producer,
            String programFile) {
        this.type = type;
        this.format = format;
        this.violationPlan = violationPlan;
        this.invariants = invariants == null ? Collections.emptyList()
                : Collections.unmodifiableList(invariants);
        this.producer = producer;
        this.programFile = programFile;
    }

    // ------------------------------------------------------------------
    // factory methods
    // ------------------------------------------------------------------

    public static WitnessModel violation(
            WitnessFormat format,
            ViolationPlan plan,
            String producer,
            String programFile) {
        return new WitnessModel(WitnessType.VIOLATION, format, plan, null, producer, programFile);
    }

    public static WitnessModel correctness(
            WitnessFormat format,
            List<CorrectnessInvariant> invariants,
            String producer,
            String programFile) {
        return new WitnessModel(WitnessType.CORRECTNESS, format, null, invariants, producer, programFile);
    }

    // ------------------------------------------------------------------
    // accessors
    // ------------------------------------------------------------------

    public WitnessType type() {
        return type;
    }

    public WitnessFormat format() {
        return format;
    }

    /**
     * @return the violation plan; only valid when {@link #type()} is {@link WitnessType#VIOLATION}
     * @throws IllegalStateException if called on a correctness witness
     */
    public ViolationPlan violationPlan() {
        if (type != WitnessType.VIOLATION) {
            throw new IllegalStateException("Not a violation witness");
        }
        return violationPlan;
    }

    /**
     * @return the loop invariants; only valid when {@link #type()} is {@link WitnessType#CORRECTNESS}
     * @throws IllegalStateException if called on a violation witness
     */
    public List<CorrectnessInvariant> invariants() {
        if (type != WitnessType.CORRECTNESS) {
            throw new IllegalStateException("Not a correctness witness");
        }
        return invariants;
    }

    /**
     * Informational — the tool that generated the witness, if declared.
     */
    public String producer() {
        return producer;
    }

    /**
     * Informational — the program file declared in the witness metadata.
     */
    public String programFile() {
        return programFile;
    }

    @Override
    public String toString() {
        return "WitnessModel{type=" + type + ", format=" + format
                + ", producer=" + producer + ", programFile=" + programFile + "}";
    }
}

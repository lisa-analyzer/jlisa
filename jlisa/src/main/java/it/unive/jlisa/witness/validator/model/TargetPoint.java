package it.unive.jlisa.witness.validator.model;

/**
 * The target waypoint of a violation witness: reaching this source location confirms that
 * the witness is correct (the property violation is reproducible).
 *
 * <p>For format v1 witnesses the target is typically the violation node's location; for v2
 * it is the explicit {@code type: target} waypoint.
 */
public record TargetPoint(String fileName, int line) {

    @Override
    public String toString() {
        return fileName + ":" + line;
    }
}

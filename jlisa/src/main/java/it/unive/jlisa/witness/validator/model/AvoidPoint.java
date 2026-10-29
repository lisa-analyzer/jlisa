package it.unive.jlisa.witness.validator.model;

/**
 * An avoid waypoint: reaching this source location means the witness path diverged from the
 * actual execution — the witness is spurious.
 */
public record AvoidPoint(String fileName, int line) {

    @Override
    public String toString() {
        return fileName + ":" + line;
    }
}

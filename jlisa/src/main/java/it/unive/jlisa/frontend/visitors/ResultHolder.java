package it.unive.jlisa.frontend.visitors;

/**
 * Interface for AST visitors that produce a result.
 *
 * @param <R> the type of the result
 */
public interface ResultHolder<R> {
    /**
     * Returns the result of visiting an AST node.
     *
     * @return the computed result
     */
    R getResult();
}
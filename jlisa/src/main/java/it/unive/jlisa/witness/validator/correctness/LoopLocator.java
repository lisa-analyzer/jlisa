package it.unive.jlisa.witness.validator.correctness;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Locates {@link Statement}s in a jLISA {@link Program} CFG by source line number.
 *
 * <p>jLISA builds its CFG from JDT source AST and retains source-line annotations via
 * {@link SourceCodeLocation}.  We exploit this to find the statement at the loop head
 * line declared in the correctness witness invariant.
 *
 * <p>Only entry-point CFGs are searched (via {@link Program#getEntryPoints()}).  For
 * SV-COMP Java benchmarks this is the {@code main} method CFG, which contains all
 * loop structures of interest.
 */
public final class LoopLocator {

    private LoopLocator() {
    }

    /**
     * Returns all {@link Statement}s in entry-point CFGs whose source line equals
     * {@code line} and whose source file name ends with {@code fileName}.
     *
     * @param program  the jLISA program built from the benchmark sources
     * @param fileName the source file name declared in the invariant (e.g. {@code "Main.java"})
     * @param line     the source line of the loop head
     * @return matching statements, empty list if none found
     */
    public static List<Statement> locate(Program program, String fileName, int line) {
        List<Statement> matches = new ArrayList<>();
        Collection<CFG> entryPoints = program.getEntryPoints();

        for (CFG cfg : entryPoints) {
            for (Statement stmt : cfg.getNodes()) {
                if (stmt.getLocation() instanceof SourceCodeLocation loc
                        && loc.getLine() == line
                        && (fileName == null || loc.getSourceFile().endsWith(fileName))) {
                    matches.add(stmt);
                }
            }
        }

        return matches;
    }

    /**
     * Convenience overload returning the first match, or {@link Optional#empty()} if none.
     */
    public static Optional<Statement> locateFirst(Program program, String fileName, int line) {
        return locate(program, fileName, line).stream().findFirst();
    }
}

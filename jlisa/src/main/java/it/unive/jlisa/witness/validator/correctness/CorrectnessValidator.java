package it.unive.jlisa.witness.validator.correctness;

import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagationWithIntervals;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.correctness.InductionChecker.CheckResult;
import it.unive.jlisa.witness.validator.correctness.InvariantParser.ParsedInvariant;
import it.unive.jlisa.witness.validator.model.CorrectnessInvariant;
import it.unive.jlisa.witness.validator.parser.WitnessModel;
import it.unive.jlisa.witness.validator.violation.ValidatorLogger;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.statement.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//import it.unive.jlisa.interprocedural.callgraph.JavaContextBasedAnalysis;

/**
 * Orchestrates correctness witness validation using jLISA abstract interpretation.
 *
 * <h3>Algorithm</h3>
 * <p>For each {@link CorrectnessInvariant} in the witness:
 * <ol>
 *   <li>Parse the {@code java_expression} string via {@link InvariantParser}</li>
 *   <li>Build the jLISA {@link Program} from the benchmark sources via {@link JavaFrontend}</li>
 *   <li>Run the LiSA analysis with {@code ConstantPropagationWithIntervals}</li>
 *   <li>Locate the loop-head {@link Statement} at the declared line via {@link LoopLocator}</li>
 *   <li>Check initiation, inductiveness, and safety via {@link InductionChecker}</li>
 * </ol>
 *
 * <p><strong>Current status:</strong> full interval-based induction checking requires
 * per-variable abstract state extraction from jLISA's analysis results.  Until that API
 * is available, the induction check returns {@code UNKNOWN} and the validator outputs
 * {@code Could not validate} rather than an incorrect verdict.
 */
public final class CorrectnessValidator {

    private static final Logger LOG = LogManager.getLogger(CorrectnessValidator.class);

    private static final String VERDICT_CORRECT = "Witness Correct";
    private static final String VERDICT_UNKNOWN = "Could not validate";

    private CorrectnessValidator() {
    }

    /**
     * Validates all invariants in the correctness witness and prints the verdict to stdout.
     *
     * @param model        the parsed witness model (must be a correctness witness)
     * @param benchmarkDir directory containing the Java benchmark sources
     * @throws ValidationException propagated to {@code Main}
     */
    public static void validate(WitnessModel model, File benchmarkDir) throws ValidationException {
        List<CorrectnessInvariant> invariants = model.invariants();
        if (invariants.isEmpty()) {
            ValidatorLogger.warn("[CORRECTNESS] No invariants in witness — could not validate");
            printVerdict(VERDICT_UNKNOWN);
            return;
        }

        ValidatorLogger.jdi("[CORRECTNESS] Building jLISA program from {}", benchmarkDir.getAbsolutePath());

        // Build the jLISA Program from the benchmark sources
        JavaFrontend frontend = buildFrontend(benchmarkDir);
        Program program = frontend.getProgram();

        // Run the analysis
        String analysisOutputDir = benchmarkDir.getAbsolutePath() + "/lisa-output/";
        runAnalysis(program, analysisOutputDir);

        // Check each invariant
        for (CorrectnessInvariant inv : invariants) {
            ValidatorLogger.jdi("[CORRECTNESS] Checking invariant: {}", inv);

            ParsedInvariant parsed;
            try {
                parsed = InvariantParser.parse(inv.value());
            } catch (ValidationException e) {
                ValidatorLogger.warn("[CORRECTNESS] Cannot parse invariant '{}': {}",
                        inv.value(), e.getMessage());
                printVerdict(VERDICT_UNKNOWN);
                return;
            }

            // Locate the loop head in the CFG via program entry-point iteration
            Optional<Statement> loopHead = LoopLocator.locateFirst(program, inv.fileName(), inv.line());
            if (loopHead.isEmpty()) {
                ValidatorLogger.warn("[CORRECTNESS] No CFG node at {}:{} — could not validate",
                        inv.fileName(), inv.line());
                printVerdict(VERDICT_UNKNOWN);
                return;
            }

            // Perform the three induction checks.
            // The full implementation requires per-variable abstract state extraction;
            // until that API is available in jLISA, each check returns UNKNOWN.
            CheckResult initiation = InductionChecker.checkInitiation(parsed, loopHead.get());
            CheckResult inductiveness = InductionChecker.checkInductiveness(parsed, loopHead.get());
            CheckResult safety = InductionChecker.checkSafety(parsed, loopHead.get());

            ValidatorLogger.jdi("[CORRECTNESS] Initiation={}, Inductiveness={}, Safety={}",
                    initiation, inductiveness, safety);

            if (initiation == CheckResult.DOES_NOT_HOLD
                    || inductiveness == CheckResult.DOES_NOT_HOLD
                    || safety == CheckResult.DOES_NOT_HOLD) {
                printVerdict(VERDICT_UNKNOWN);
                return;
            }

            if (initiation == CheckResult.UNKNOWN
                    || inductiveness == CheckResult.UNKNOWN
                    || safety == CheckResult.UNKNOWN) {
                printVerdict(VERDICT_UNKNOWN);
                return;
            }
        }

        // All invariants confirmed
        ValidatorLogger.result(VERDICT_CORRECT);
        printVerdict(VERDICT_CORRECT);
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private static JavaFrontend buildFrontend(File benchmarkDir) throws ValidationException {
        try {
            JavaFrontend frontend = new JavaFrontend();
            List<String> sources = new ArrayList<>();
            File[] javaFiles = benchmarkDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles == null || javaFiles.length == 0) {
                throw new ValidationException("No .java sources in " + benchmarkDir.getAbsolutePath());
            }
            for (File f : javaFiles) {
                sources.add(f.getAbsolutePath());
            }
            ValidatorLogger.jdi("[CORRECTNESS] Parsing {} source file(s) with jLISA", sources.size());
            frontend.parseFromListOfFile(sources);
            return frontend;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("jLISA program construction failed: " + e.getMessage(), e);
        }
    }

    private static void runAnalysis(Program program, String workdir) throws ValidationException {
        try {
            Files.createDirectories(java.nio.file.Path.of(workdir));
        } catch (Exception ignored) {
        }

        // Configuration follows jLISA's Main.runAnalysis() pattern exactly
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = workdir;
        conf.outputs.add(new JSONResults<>());
        conf.outputs.add(new JSONReportDumper());
        conf.interproceduralAnalysis = null; // new JavaContextBasedAnalysis<>(150);
        conf.callGraph = new JavaRTACallGraph();
        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
        conf.semanticChecks.add(new AssertChecker<>());
        conf.analysis = new Reachability<>(new SimpleAbstractDomain<>(
                new JavaFieldSensitivePointBasedHeap(),
                new ConstantPropagationWithIntervals(),
                new JavaInferredTypes()));

        try {
            ValidatorLogger.jdi("[CORRECTNESS] Running LiSA analysis (workdir={})", workdir);
            new LiSA(conf).run(program);
            ValidatorLogger.jdi("[CORRECTNESS] Analysis complete");
        } catch (Exception e) {
            throw new ValidationException("LiSA analysis failed: " + e.getMessage(), e);
        }
    }

    private static void printVerdict(String verdict) {
        System.out.println(verdict);
        System.out.flush();
    }
}

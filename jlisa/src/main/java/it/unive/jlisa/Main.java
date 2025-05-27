package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.*;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.util.Arrays;


public class Main {
    public static void main(String[] args) throws IOException {
        //String source = "public class Hello { public static void main() { System.out.println(\"Hello World :)\"); } }";

        //ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // NOTE: JLS8 is deprecated. getJLSLatest will return JDK23
        //parser.setSource(source.toCharArray());
        //parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // Define options
        Options options = new Options();

        Option helpOption = new Option("h", "help", false, "Print this help message");
        Option sourceOption = Option.builder("s")
                .longOpt("source")
                .hasArgs()
                .desc("Source files (e.g. -s file1 file2 file3)")
                .required(false) // Will validate manually if help is not used
                .build();

        Option outdirOption = Option.builder("o")
                .longOpt("outdir")
                .hasArg()
                .desc("Output directory")
                .required(false)
                .build();

        Option logLevel = Option.builder("l")
                .longOpt("log-level")
                .hasArg()
                .desc("Log level: (INFO, DEBUG, WARNING, ERROR, FATAL, TRACE, ALL, OFF)")
                .required(false)
                .build();
        options.addOption(helpOption);
        options.addOption(sourceOption);
        options.addOption(outdirOption);
        options.addOption(logLevel);
        // Create parser and formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        String[] sources = new String[0];
        String outdir = "";
        try {
            CommandLine cmd = parser.parse(options, args);

            // Handle help
            if (cmd.hasOption("h") || args.length == 0) {
                formatter.printHelp("jlisa", options, true);
                System.exit(0);
            }

            // Check required manually if help was not triggered
            if (!cmd.hasOption("s") || !cmd.hasOption("o")) {
                throw new ParseException("Missing required options: --source and/or --outdir");
            }
            if (cmd.hasOption("l")) {
                String log4jLevelName = cmd.getOptionValue("l").toUpperCase();
                Level level = Level.getLevel(log4jLevelName);
                if (level == null) {
                    throw new ParseException("Invalid log level: " + log4jLevelName);
                }
                LogManager.setLogLevel(level);
            }

            sources = cmd.getOptionValues("s");
            outdir = cmd.getOptionValue("o");
            if (!outdir.endsWith("/")) {
                outdir += "/";
            }
            // Output
            System.out.println("Source files:");
            for (String file : sources) {
                System.out.println(" - " + file);
            }

            System.out.println("Output directory: " + outdir);

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            formatter.printHelp("jlisa", options, true);
            System.exit(1);
        }

        JavaFrontend frontend = new JavaFrontend();
        frontend.parseFromListOfFile(Arrays.stream(sources).toList());
        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            throw new RuntimeException("Some errors occurred. Check " + outdir + "errors.csv file.");
        }
        Program p = frontend.getProgram();

        System.out.println(p);
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = outdir;
        conf.serializeResults = false;
        conf.jsonOutput = false;
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
        conf.callGraph = new RTACallGraph();
        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
        conf.optimize = false;

        FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap().bottom();
        TypeEnvironment<InferredTypes> type = new TypeEnvironment<>(new InferredTypes());
        ValueEnvironment<IntegerConstantPropagation> domain = new ValueEnvironment<>(new IntegerConstantPropagation());
        conf.abstractState = new SimpleAbstractState<>(heap, domain, type);


        LiSA lisa = new LiSA(conf);
        lisa.run(p);

    }
}
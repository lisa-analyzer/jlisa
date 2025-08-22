package it.unive.jlisa;

import java.io.IOException;
import java.util.Arrays;

import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.lisa.analysis.value.ValueDomain;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

import it.unive.jlisa.analysis.ConstantPropagation;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import org.apache.logging.log4j.Logger;


public class Main {

    private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(Main.class);
    	
    public static void main(String[] args) throws IOException, ParseException, ParsingException {
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

        Option checker = Option.builder("c")
                .longOpt("checker")
                .hasArg()
                .desc("Checker: (Assert)")
                .required(false)
                .build();

        Option numericalDomainOption = Option.builder("n")
                .longOpt("numericalDomain")
                .hasArg()
                .desc("Numerical domain: (ConstantPropagation)")
                .required(false)
                .build();

        Option mode = Option.builder("m")
                .longOpt("mode")
                .hasArg()
                .desc("Execution mode: (Statistics, Debug [DEFAULT])")
                .required(false)
                .build();

        options.addOption(helpOption);
        options.addOption(sourceOption);
        options.addOption(outdirOption);
        options.addOption(logLevel);
        options.addOption(checker);
        options.addOption(numericalDomainOption);
        options.addOption(mode);
        // Create parser and formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        String[] sources = new String[0];
        String outdir = "";
        String checkerName ="", numericalDomain="", executionMode="Debug";
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

            // Check required manually if help was not triggered
            if (!cmd.hasOption("n") || !cmd.hasOption("o")) {
                throw new ParseException("Missing required options: --numericalDomain");
            }
            if (cmd.hasOption("l")) {
                String log4jLevelName = cmd.getOptionValue("l").toUpperCase();
                Level level = Level.getLevel(log4jLevelName);
                if (level == null) {
                    throw new ParseException("Invalid log level: " + log4jLevelName);
                }
                LogManager.setLogLevel(level);
            }
            
            checkerName = cmd.getOptionValue("c");
            numericalDomain = cmd.getOptionValue("n");

            sources = cmd.getOptionValues("s");
            outdir = cmd.getOptionValue("o");
            if (!outdir.endsWith("/")) {
                outdir += "/";
            }
            // Output
            LOG.info("Source files:");
            for (String file : sources) {
                LOG.info(" - " + file);
            }

            LOG.info("Output directory: " + outdir);

            if(cmd.hasOption("m"))
                executionMode = cmd.getOptionValue("m");

        } catch (ParseException e) {
            LOG.error("Error: " + e.getMessage());
            formatter.printHelp("jlisa", options, true);
            System.exit(1);
        }

        switch(executionMode) {
            case "Debug":
                runDebug(sources, outdir, checkerName, numericalDomain);
                break;
            case "Statistics":
                runStatistics(sources, outdir, checkerName, numericalDomain);
                break;
            default:
                LOG.error("Unknown execution mode: " + executionMode);
                System.exit(1);
        }
    }
    private static void runDebug(String[] sources, String outdir, String checkerName, String numericalDomain) throws IOException, ParseException, ParsingException {
        JavaFrontend frontend = runFrontend(sources);
        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            LOG.error("Some errors occurred during the parsing, reporting the first one");
            throw frontend.getParserContext().getExceptions().getFirst();
        }
        runAnalysis(outdir, checkerName, numericalDomain, frontend);
    }

    private static void runStatistics(String[] sources, String outdir, String checkerName, String numericalDomain) {
        JavaFrontend frontend = null;
        try {
            frontend = runFrontend(sources);
            if (!frontend.getParserContext().getExceptions().isEmpty()) {
                CSVExceptionWriter.writeCSV(outdir + "frontend.csv", frontend.getParserContext().getExceptions());
                LOG.error("Some errors occurred during the parsing. Check " + outdir + "frontend.csv file.");
                System.exit(1);
            }
        }
        catch(Throwable e) {
                CSVExceptionWriter.writeCSV(outdir + "frontend-noparsing.csv",e);
                LOG.error("Some errors occurred in the frontend outside the parsing phase. Check " + outdir + "-noparsing.csv file.");
                System.exit(1);
            }
        try{
            runAnalysis(outdir, checkerName, numericalDomain, frontend);
        } catch (Throwable e) {
            CSVExceptionWriter.writeCSV(outdir + "analysis.csv", e.getCause()!=null? e.getCause(): e);
            LOG.error("Some errors occurred during the analysis. Check " + outdir + "analysis.csv file.");
            System.exit(1);
        }
    }

    private static JavaFrontend runFrontend(String[] sources) throws IOException, ParsingException {
        JavaFrontend frontend = null;
        try {
            frontend = new JavaFrontend();
            frontend.parseFromListOfFile(Arrays.stream(sources).toList());
            return frontend;
        }
        catch(Exception e) {
            if (frontend != null && !frontend.getParserContext().getExceptions().isEmpty()) {
                LOG.error("Some errors occurred during the parsing, reporting the first one");
                throw frontend.getParserContext().getExceptions().getFirst();
            }
            else throw e;
        }
    }

    private static void runAnalysis(String outdir, String checkerName, String numericalDomain, JavaFrontend frontend) throws ParseException {
        Program p = frontend.getProgram();
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = outdir;
        conf.serializeResults = false;
        conf.jsonOutput = true;
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
        conf.callGraph = new RTACallGraph();
        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
        conf.optimize = false;
        switch (checkerName) {
            case "Assert": conf.semanticChecks.add(new AssertChecker()); break;
            case "": break;
            default: throw new ParseException("Invalid checker name: " + checkerName);
        }
        ValueDomain<?> domain;
        switch (numericalDomain) {
            case "ConstantPropagation": domain = new ConstantPropagation(); break;
            default: throw new ParseException("Invalid numerical domain name: " + numericalDomain);
        }

        conf.analysis = new SimpleAbstractDomain<>(
                new FieldSensitivePointBasedHeap(),
                domain,
                new InferredTypes());


        LiSA lisa = new LiSA(conf);
        lisa.run(p);
    }
}
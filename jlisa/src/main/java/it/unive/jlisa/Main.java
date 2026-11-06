package it.unive.jlisa;

import it.unive.jlisa.cli.CheckCommand;
import it.unive.jlisa.cli.FindBugCommand;
import it.unive.jlisa.cli.ValidateCommand;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import org.apache.commons.cli.ParseException;

/**
 * The entry point of jlisa: dispatches to its sub-commands.
 *
 * <pre>{@code
 * jlisa check    -s <inputs...> -o <dir> -n <domain> [--svcomp-property <file.prp>]... [check options]
 * jlisa validate -s <inputs...> --svcomp-property <file.prp> -w <witness> [validate options]
 * jlisa find-bug -s <inputs...> --svcomp-property <file.prp> [find-bug options]
 * jlisa -h | --help | -v | --version
 * }</pre>
 *
 * Each sub-command prints its own options with {@code -h}.
 */
public final class Main {

	private Main() {
	}

	/**
	 * The entry point of jlisa.
	 *
	 * @param args the command-line arguments: the sub-command, then its
	 *                 arguments
	 *
	 * @throws IOException      if a source cannot be read ({@code check})
	 * @throws ParseException   if the checker or the domain is not known
	 *                              ({@code check})
	 * @throws ParsingException if the frontend fails ({@code check})
	 */
	public static void main(
			String[] args)
			throws IOException,
			ParseException,
			ParsingException {
		if (args.length == 0) {
			usage(System.err);
			System.exit(1);
			return;
		}
		String[] rest = Arrays.copyOfRange(args, 1, args.length);
		switch (args[0]) {
		case "check":
			CheckCommand.run(rest);
			break;
		case "validate":
			ValidateCommand.run(rest);
			break;
		case "find-bug":
			FindBugCommand.run(rest);
			break;
		case "-h":
		case "--help":
			usage(System.out);
			break;
		case "-v":
		case "--version":
			System.out.println(versionLine());
			break;
		default:
			System.err.println("Error: unknown sub-command: " + args[0]);
			usage(System.err);
			System.exit(1);
		}
	}

	/**
	 * Returns the line that {@code --version} prints.
	 *
	 * @return the line, with the version of jlisa
	 */
	public static String versionLine() {
		return "JLiSA version: " + Version.get();
	}

	private static void usage(
			PrintStream out) {
		out.println("usage: jlisa <sub-command> [options]");
		out.println();
		out.println("Sub-commands:");
		out.println("  check      analyse a program and check its properties");
		out.println("  validate   validate a witness of an SV-COMP task");
		out.println("  find-bug   look for inputs that violate the property of an SV-COMP task");
		out.println();
		out.println("Options:");
		out.println("  -h, --help      print this help message");
		out.println("  -v, --version   print the version of jlisa");
		out.println();
		out.println("Run 'jlisa <sub-command> -h' for the options of a sub-command.");
	}
}

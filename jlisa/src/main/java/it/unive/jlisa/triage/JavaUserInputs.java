package it.unive.jlisa.triage;

import it.unive.jlisa.program.java.constructs.random.NextBoolean;
import it.unive.jlisa.program.java.constructs.random.NextBytes;
import it.unive.jlisa.program.java.constructs.random.NextDouble;
import it.unive.jlisa.program.java.constructs.random.NextFloat;
import it.unive.jlisa.program.java.constructs.random.NextInt;
import it.unive.jlisa.program.java.constructs.random.NextLong;
import it.unive.jlisa.program.java.constructs.reader.ReaderRead;
import it.unive.jlisa.program.java.constructs.verifier.VerifierNondet;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.triage.inputs.UserInputs;
import java.util.Set;

/**
 * The natives of the Java libraries whose unknown results are inputs provided
 * by the user (see {@link UserInputs}): the values read from readers, the
 * random numbers, and the nondeterministic values of
 * {@code org.sosy_lab.sv_benchmarks.Verifier}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaUserInputs
		implements
		UserInputs {

	/**
	 * The constructs of the natives producing user inputs.
	 */
	private static final Set<Class<?>> INPUTS = Set.of(NextBoolean.class, NextBytes.class, NextDouble.class,
			NextFloat.class, NextInt.class, NextLong.class, ReaderRead.class, VerifierNondet.class);

	@Override
	public boolean isUserInput(
			NativeCFG target) {
		return INPUTS.contains(target.getConstruct());
	}
}

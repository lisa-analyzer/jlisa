package it.unive.jlisa.program;

import it.unive.jlisa.program.java.constructs.object.ObjectClassInitializer;
import it.unive.jlisa.program.java.constructs.object.ObjectConstructor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.type.TypeSystem;

public class JavaProgram extends Program {
	private ClassUnit rootClassUnit;

	/**
	 * Builds an empty program.
	 *
	 * @param features the language-specific features, algorithms and semantics
	 *                     of this program
	 * @param types    the type system knowing about the types that appear in
	 *                     the program
	 */
	public JavaProgram(
			LanguageFeatures features,
			TypeSystem types) {
		super(features, types);
		SourceCodeLocationManager locationManager = new SourceCodeLocationManager("$java-runtime/Object");
		rootClassUnit = new ClassUnit(locationManager.getRoot(), this, "Object", false);
		JavaClassType.lookup("Object", rootClassUnit);
		rootClassUnit.addCodeMember(new ObjectClassInitializer(locationManager.nextRow(), rootClassUnit));
		rootClassUnit.addInstanceCodeMember(new ObjectConstructor(locationManager.nextRow(), rootClassUnit));
		this.addUnit(rootClassUnit);
	}
}

package it.unive.jlisa.springed.p1;

import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.constructs.WebAnnotation;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;
import java.util.List;

public sealed interface P1 permits P1Impl {
	Registry produceRegistry(
			Unit[] p);

	List<ClassUnit> getControllerClasses(
			Unit[] units);

	WebAnnotation extractAnnotation(
			CodeMember method);

	WebAnnotation createNewAnnotation(
			Annotation from);
}

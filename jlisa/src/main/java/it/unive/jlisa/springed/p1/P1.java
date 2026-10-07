package it.unive.jlisa.springed.p1;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;

import java.util.Collection;
import java.util.List;

public interface P1 {
	Registry p1(
			Unit[] p);

    List<ClassUnit> getControllers(
			Unit[] units);

    RequestMapping getMappingForMethod(
            ClassUnit controller, CodeMember method);

    RequestMapping doMetaResolution(
            Collection<Annotation> annotations);

    RequestMapping mergeMappings(
            RequestMapping controllerMapping, RequestMapping methodMapping);

    RequestMapping resolveEmptyMapping(
            RequestMapping mapping);
}

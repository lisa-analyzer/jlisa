package it.unive.jlisa.springed.p1;

import static it.unive.jlisa.springed.p1.util.P1Util.getHttpMethod;

import it.unive.jlisa.springed.p1.constructs.Mapping;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.constructs.WebAnnotation;
import it.unive.jlisa.springed.p1.util.P1Util;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class P1Impl implements P1 {

	private final List<String> controllerAnnotationNames = List.of(
			"RestController",
			"Controller");

	private final List<String> restAnnotationNames = List.of(
			"RequestMapping",
			"GetMapping",
			"PostMapping",
			"PutMapping",
			"DeleteMapping",
			"PatchMapping");

	@Override
	public Registry produceRegistry(
			Unit[] p) {
		Registry registry = new Registry();

		for (ClassUnit classUnit : this.getControllerClasses(p)) {
			for (CodeMember method : classUnit.getInstanceCodeMembers(false)) {
				WebAnnotation webAnnotation = this.extractAnnotation(method);

				if (webAnnotation != null) {
					Mapping mapping = new Mapping(method, webAnnotation);
					registry.insert(mapping);
				}
			}
		}

		return registry;
	}

	@Override
	public List<ClassUnit> getControllerClasses(
			Unit[] units) {
		List<ClassUnit> classes = new ArrayList<>();

		for (Unit unit : units) {
			if (unit instanceof ClassUnit) {
				classes.add((ClassUnit) unit);
			}
		}

		List<ClassUnit> controllers = new ArrayList<>();
		for (ClassUnit classUnit : classes) {
			for (Annotation ann : classUnit.getAnnotationList()) {

				String annName = ann.getAnnotationName();
				if (this.controllerAnnotationNames.contains(annName)) {
					controllers.add(classUnit);
				}
			}
		}

		return controllers;
	}

	@Override
	public WebAnnotation extractAnnotation(
			CodeMember method) {
		WebAnnotation webAnnotation = null;

		Collection<Annotation> anns = method.getDescriptor().getAnnotationList();
		for (Annotation ann : anns) {
			String annName = ann.getAnnotationName();

			if (this.restAnnotationNames.contains(annName)) {
				webAnnotation = createNewAnnotation(ann);
			}
		}

		return webAnnotation;
	}

	@Override
	public WebAnnotation createNewAnnotation(
			Annotation annotation) {
		String httpMethod = getHttpMethod(annotation);
		String path = P1Util.getPath(annotation);
		Map<String, Object> params = null;
		Map<String, Object> headers = null;

		return new WebAnnotation(httpMethod, path, params, headers);
	}

}

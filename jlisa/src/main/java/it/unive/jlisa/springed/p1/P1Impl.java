package it.unive.jlisa.springed.p1;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.springed.exceptions.PathMergeException;
import it.unive.jlisa.springed.p1.util.RequestMappingBuilder;
import it.unive.jlisa.springed.p1.util.RequestMappingMerger;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;

import java.util.*;

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

	private final ParserContext parserContext;

	public P1Impl(ParserContext parserContext) {
		this.parserContext = parserContext;
	}

	public Registry p1(
			Unit[] p) {
		Registry registry = new Registry();
		List<ClassUnit> controllers = this.getControllers(p);

		for (ClassUnit controller : controllers) {
			Collection<CodeMember> methods = controller.getInstanceCodeMembers(false);

			for (CodeMember method : methods) {
				try {
					RequestMapping requestMapping = this.getMappingForMethod(controller, method);

					if (requestMapping != null) {
						RegistryRecord registryRecord = new RegistryRecord(method, requestMapping);
						registry.insert(registryRecord);
					}
				} catch (PathMergeException e) {
					this.parserContext.addException(e);
				}
			}
		}

		return registry;
	}

	public RequestMapping getMappingForMethod(ClassUnit controller, CodeMember method) {

		Collection<Annotation> methodAnnotations = method.getDescriptor().getAnnotationList();
		RequestMapping methodMapping = doMetaResolution(methodAnnotations);

		if (methodMapping != null) {
			Collection<Annotation> controllerAnnotations = controller.getAnnotationList();
			RequestMapping controllerMapping = doMetaResolution(controllerAnnotations);

			if (controllerMapping != null) {
				methodMapping = mergeMappings(controllerMapping, methodMapping);
			}

			methodMapping = resolveEmptyMapping(methodMapping);
		}

		return methodMapping;
	}

	public RequestMapping mergeMappings(RequestMapping controllerMapping, RequestMapping methodMapping) {
		return new RequestMappingMerger().merge(controllerMapping, methodMapping);
	}

	public RequestMapping resolveEmptyMapping(RequestMapping mapping) {
		Set<String> paths = mapping.getPaths();
		boolean mappingHasNoPaths = paths.isEmpty() || paths.equals(Set.of(""));

		if (!mappingHasNoPaths) {
			return mapping;
		}

		return new RequestMapping(mapping.getMethods(), new LinkedHashSet<>(List.of("", "/")), mapping.getParams(),
				mapping.getHeaders(), mapping.getConsumes(), mapping.getProduces(), mapping.getVersion());
	}


	public RequestMapping doMetaResolution(Collection<Annotation> annotations) {
		for (Annotation annotation : annotations) {
			String annotationName = annotation.getAnnotationName();
			if (this.restAnnotationNames.contains(annotationName)) {
                return RequestMappingBuilder.build(annotation);
			}
		}

		return null;
	}

	public List<ClassUnit> getControllers(
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
}

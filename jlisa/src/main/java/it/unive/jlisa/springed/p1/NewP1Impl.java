package it.unive.jlisa.springed.p1;

import it.unive.jlisa.springed.p1.constructs.Mapping;
import it.unive.jlisa.springed.p1.constructs.Registry;
import it.unive.jlisa.springed.p1.constructs.RequestMapping;
import it.unive.jlisa.springed.p1.constructs.WebAnnotation;
import it.unive.jlisa.springed.p1.util.P1Util;
import it.unive.jlisa.springed.p1.util.RequestMappingBuilder;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;

import java.util.*;

import static it.unive.jlisa.springed.p1.util.P1Util.getHttpMethod;

public final class NewP1Impl {

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

	public Registry produceRegistry(
			Unit[] p) {
		Registry registry = new Registry();

		for (ClassUnit classUnit : this.getControllers(p)) {
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

	public List<RequestMapping> p1(
			Unit[] p) {
		List<RequestMapping> requestMappings = new ArrayList<>();
		List<ClassUnit> controllers = this.getControllers(p);

		for (ClassUnit controller : controllers) {
			Collection<CodeMember> methods = controller.getInstanceCodeMembers(false);

			for (CodeMember method : methods) {
				RequestMapping requestMapping = this.getMappingForMethod(controller, method);

				if (requestMapping != null) {
					requestMappings.add(requestMapping);
				}
			}
		}

		return requestMappings;
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
		}

		return methodMapping;
	}

	private RequestMapping mergeMappings(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> paths = handleMergePathPatterns(controllerMapping, methodMapping);
		Set<String> methods = mergeMethods(controllerMapping, methodMapping);
		Set<String> params = mergeParams(controllerMapping, methodMapping);
		Set<String> headers = mergeHeaders(controllerMapping, methodMapping);
		Set<String> consumes = mergeConsumes(controllerMapping, methodMapping);
		Set<String> produces = mergeProduces(controllerMapping, methodMapping);
		Set<String> version = mergeVersion(controllerMapping, methodMapping);

		return new RequestMapping(methods, paths, params, headers, consumes, produces, version);
	}

	private Set<String> handleMergePathPatterns(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerPaths = controllerMapping.getPaths();
		Set<String> methodPaths = methodMapping.getPaths();

		Set<String> rootPathPatterns = new LinkedHashSet<>(List.of("", "/"));

		if (controllerPaths.isEmpty() && methodPaths.isEmpty()) {
			return rootPathPatterns;
		} else if (methodPaths.isEmpty()) {
			return controllerPaths;
		} else if (controllerPaths.isEmpty()) {
			return methodPaths;
		}

		Set<String> mergedPaths = new LinkedHashSet<>();

		for (String controllerPath : controllerPaths) {
			for (String methodPath : methodPaths) {
				String mergedPath = mergePathPatterns(controllerPath, methodPath);

				if (mergedPath != null) {
					mergedPaths.add(mergedPath);
				}
			}
		}

		return mergedPaths;
	}

	private String mergePathPatterns(String controllerPath, String methodPath) {
		if (controllerPath.isEmpty() && methodPath.isEmpty()) {
			return "";
		} else if (controllerPath.isEmpty()) {
			return methodPath;
		} else if (methodPath.isEmpty()) {
			return controllerPath;
		}

		String mergedPath = applyMR1(controllerPath, methodPath);
		if (mergedPath != null) {
			return mergedPath;
		}

		mergedPath = applyMR2(controllerPath, methodPath);
		if (mergedPath != null) {
			return mergedPath;
		}

		return applyMR3(controllerPath, methodPath);
	}

	private String applyMR1(String controllerPath, String methodPath) {
		boolean bothPathsAreFixed = isPathFixed(controllerPath) && isPathFixed(methodPath);
		if (bothPathsAreFixed) {
			return concatenate(controllerPath, methodPath);
		}

		boolean controllerFixedButMethodVariable = isPathFixed(controllerPath) && containsPathVariable(methodPath);
		if (controllerFixedButMethodVariable) {
			return concatenate(controllerPath, methodPath);
		}

		return null;
	}

	private String applyMR2(String controllerPath, String methodPath) {
		boolean controllerIsJustWildcard = isJustWildcard(controllerPath) && isPathFixed(methodPath);
		if (controllerIsJustWildcard) {
			return concatenate("", methodPath);
		}

		boolean controllerEndsWithWildcardButMethodFixed = endsWithWildcard(controllerPath) && isPathFixed(methodPath);
		if (controllerEndsWithWildcardButMethodFixed) {
			String controllerTrimmed = controllerPath.substring(0, controllerPath.length() - 2);
			return concatenate(controllerTrimmed, methodPath);
		}

		return null;
	}

	private String applyMR3(String controllerPath, String methodPath) {
		boolean controllerIsJustCatchAll = isJustCatchAll(controllerPath) && isPathFixed(methodPath);
		if (controllerIsJustCatchAll) {
			return concatenate("", methodPath);
		}

		boolean controllerEndsWithCatchAllButMethodFixed = endsWithCatchAll(controllerPath) && isPathFixed(methodPath);
		if (controllerEndsWithCatchAllButMethodFixed) {
			String controllerTrimmed = controllerPath.substring(0, controllerPath.length() - 3);
			return concatenate(controllerTrimmed, methodPath);
		}

		return null;
	}

	private String concatenate(String controllerPath, String methodPath) {
		if (!methodPath.isEmpty() && !methodPath.startsWith("/")) {
			methodPath = "/" + methodPath;
		}

		return controllerPath + methodPath;
	}

	private boolean isPathFixed(String path) {
		for (String specialChar : List.of("*", "?", "{", "}")) {
			if (path.contains(specialChar)) {
				return false;
			}
		}

		return true;
	}

	private boolean containsPathVariable(String path) {
		return path.contains("{") || path.contains("}");
	}

	private boolean endsWithWildcard(String path) {
		return path.endsWith("/*");
	}

	private boolean endsWithCatchAll(String path) {
		return path.endsWith("/**");
	}

	private boolean isJustWildcard(String path) {
		return path.equals("/*");
	}

	private boolean isJustCatchAll(String path) {
		return path.equals("/**");
	}

	private Set<String> mergeMethods(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerMethods = controllerMapping.getMethods();
		Set<String> methodMethods = methodMapping.getMethods();

		if (methodMethods.isEmpty()) {
			return controllerMethods;
		} else if (controllerMethods.isEmpty()) {
			return methodMethods;
		}

		Set<String> merged = new LinkedHashSet<>(controllerMethods);
		merged.addAll(methodMethods);
		return merged;
	}

	private Set<String> mergeParams(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerParams = controllerMapping.getParams();
		Set<String> methodParams = methodMapping.getParams();

		if (methodParams.isEmpty()) {
			return controllerParams;
		} else if (controllerParams.isEmpty()) {
			return methodParams;
		}

		Set<String> merged = new LinkedHashSet<>(controllerParams);
		merged.addAll(methodParams);
		return merged;
	}

	private Set<String> mergeHeaders(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerHeaders = controllerMapping.getHeaders();
		Set<String> methodHeaders = methodMapping.getHeaders();

		if (methodHeaders.isEmpty()) {
			return controllerHeaders;
		} else if (controllerHeaders.isEmpty()) {
			return methodHeaders;
		}

		Set<String> merged = new LinkedHashSet<>(controllerHeaders);
		merged.addAll(methodHeaders);
		return merged;
	}

	private Set<String> mergeConsumes(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerConsumes = controllerMapping.getConsumes();
		Set<String> methodConsumes = methodMapping.getConsumes();

		if (!methodConsumes.isEmpty()) {
			return methodConsumes;
		}

		return controllerConsumes;
	}

	private Set<String> mergeProduces(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerProduces = controllerMapping.getProduces();
		Set<String> methodProduces = methodMapping.getProduces();

		if (!methodProduces.isEmpty()) {
			return methodProduces;
		}

		return controllerProduces;
	}

	private Set<String> mergeVersion(RequestMapping controllerMapping, RequestMapping methodMapping) {
		Set<String> controllerVersion = controllerMapping.getVersion();
		Set<String> methodVersion = methodMapping.getVersion();

		if (!methodVersion.isEmpty()) {
			return methodVersion;
		}

		return controllerVersion;
	}

	private RequestMapping doMetaResolution(Collection<Annotation> annotations) {
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


	public WebAnnotation createNewAnnotation(
			Annotation annotation) {
		String httpMethod = getHttpMethod(annotation);
		String path = P1Util.getPath(annotation);
		Map<String, Object> params = null;
		Map<String, Object> headers = null;

		return new WebAnnotation(httpMethod, path, params, headers);
	}
}

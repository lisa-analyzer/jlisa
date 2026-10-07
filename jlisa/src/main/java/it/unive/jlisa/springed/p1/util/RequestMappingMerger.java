package it.unive.jlisa.springed.p1.util;

import it.unive.jlisa.springed.exceptions.PathMergeException;
import it.unive.jlisa.springed.p1.RequestMapping;

import java.util.LinkedHashSet;
import java.util.Set;

public class RequestMappingMerger {

    public RequestMapping merge(RequestMapping controllerMapping, RequestMapping methodMapping) {
        Set<String> paths = mergePathPatterns(controllerMapping, methodMapping);
        Set<String> methods = mergeMethods(controllerMapping, methodMapping);
        Set<String> params = mergeParams(controllerMapping, methodMapping);
        Set<String> headers = mergeHeaders(controllerMapping, methodMapping);
        Set<String> consumes = mergeConsumes(controllerMapping, methodMapping);
        Set<String> produces = mergeProduces(controllerMapping, methodMapping);
        String version = mergeVersion(controllerMapping, methodMapping);

        return new RequestMapping(methods, paths, params, headers, consumes, produces, version);
    }

    private Set<String> mergePathPatterns(RequestMapping controllerMapping, RequestMapping methodMapping) {
        Set<String> controllerPaths = controllerMapping.getPaths();
        Set<String> methodPaths = methodMapping.getPaths();

        Set<String> mergedPaths = this.resolveEmptyPathSets(controllerPaths, methodPaths);
        if (!mergedPaths.isEmpty()) return mergedPaths;

        for (String controllerPath : controllerPaths) {
            for (String methodPath : methodPaths) {
                mergedPaths.add(mergePaths(controllerPath, methodPath));
            }
        }

        return mergedPaths;
    }

    private String mergePaths(String controllerPath, String methodPath) {
        String mergedPath = resolveEmptyPath(controllerPath, methodPath);
        if (mergedPath != null) return mergedPath;

        rejectUnmergeable(controllerPath, methodPath);
        rejectFileNameMerge(controllerPath, methodPath);

        mergedPath = mergeByMethodPath(controllerPath, methodPath);
        if (mergedPath == null) mergedPath = mergeByWildcard(controllerPath, methodPath);
        if (mergedPath == null) mergedPath = mergeByConcatenation(controllerPath, methodPath);
        if (mergedPath == null) mergedPath = mergeByFileName(controllerPath, methodPath);

        return mergedPath;
    }

    private Set<String> resolveEmptyPathSets(Set<String> controllerPaths, Set<String> methodPaths) {
        boolean controllerHasNoPaths = controllerPaths.isEmpty() || controllerPaths.equals(Set.of(""));
        boolean methodHasNoPaths = methodPaths.isEmpty() || methodPaths.equals(Set.of(""));

        if (controllerHasNoPaths && methodHasNoPaths) {
            return Set.of("");
        } else if (methodHasNoPaths) {
            return controllerPaths;
        } else if (controllerHasNoPaths) {
            return methodPaths;
        }

        return new LinkedHashSet<>();
    }

    private String resolveEmptyPath(String controllerPath, String methodPath) {
        boolean controllerPathIsEmpty = controllerPath.isEmpty();
        boolean methodPathIsEmpty = methodPath.isEmpty();

        if (controllerPathIsEmpty && methodPathIsEmpty) {
            return "";
        } else if (controllerPathIsEmpty) {
            return methodPath;
        } else if (methodPathIsEmpty) {
            return controllerPath;
        }

        return null;
    }

    private void rejectUnmergeable(String controllerPath, String methodPath) {
        boolean bothPathsAreEqualWithPathVariable = controllerPath.equals(methodPath) && containsPathVariable(controllerPath);
        boolean bothPathsAreJustCatchAll = controllerPath.equals("/**") && methodPath.equals("/**");
        boolean controllerHasCatchAllVariable = controllerPath.contains("{*");

        if (bothPathsAreEqualWithPathVariable || bothPathsAreJustCatchAll || controllerHasCatchAllVariable) {
            throw new PathMergeException(
                    "Merging paths: " + controllerPath + " and " + methodPath + " is not valid", controllerPath, methodPath);
        }
    }

    private void rejectFileNameMerge(String controllerPath, String methodPath) {
        boolean controllerIsFilePattern = isFilePattern(controllerPath);
        boolean methodFitsIntoController = !controllerPath.equals(methodPath) && fitsInto(methodPath, controllerPath);
        boolean bothPathsHaveSpecificExtension = hasSpecificExtension(controllerPath) && hasSpecificExtension(methodPath);
        boolean bothPathsHaveDirectory = controllerPath.contains("/") && methodPath.contains("/");
        boolean methodDirectoryFitsIntoControllerDirectory = bothPathsHaveDirectory
                && fitsInto(directoryOf(methodPath), directoryOf(controllerPath));

        if (controllerIsFilePattern && !methodFitsIntoController
                && (bothPathsHaveSpecificExtension || !methodDirectoryFitsIntoControllerDirectory)) {
            throw new PathMergeException(
                    "Merging paths: " + controllerPath + " and " + methodPath + " is not valid", controllerPath, methodPath);
        }
    }

    private String mergeByMethodPath(String controllerPath, String methodPath) {
        boolean controllerHasPathVariable = containsPathVariable(controllerPath);
        boolean methodFitsIntoController = !controllerPath.equals(methodPath) && fitsInto(methodPath, controllerPath);

        if (!controllerHasPathVariable && methodFitsIntoController) {
            return methodPath;
        }

        return null;
    }

    private String mergeByWildcard(String controllerPath, String methodPath) {
        boolean controllerEndsWithWildcard = controllerPath.endsWith("/*");
        boolean controllerEndsWithCatchAll = controllerPath.endsWith("/**");

        if (controllerEndsWithWildcard) {
            return concatenate(trimEnding(controllerPath, "/*"), methodPath);
        } else if (controllerEndsWithCatchAll) {
            return concatenate(trimEnding(controllerPath, "/**"), methodPath);
        }

        return null;
    }

    private String mergeByConcatenation(String controllerPath, String methodPath) {
        boolean controllerIsFilePattern = isFilePattern(controllerPath);

        if (!controllerIsFilePattern) {
            return concatenate(controllerPath, methodPath);
        }

        return null;
    }

    private String mergeByFileName(String controllerPath, String methodPath) {
        boolean controllerIsFilePattern = isFilePattern(controllerPath);
        boolean controllerHasSpecificExtension = hasSpecificExtension(controllerPath);

        if (controllerIsFilePattern && controllerHasSpecificExtension) {
            return replaceAnyFileName(controllerPath, methodPath);
        } else if (controllerIsFilePattern) {
            return methodPath;
        }

        return null;
    }

    private String concatenate(String controllerPath, String methodPath) {
        boolean controllerEndsWithSeparator = controllerPath.endsWith("/");
        boolean methodStartsWithSeparator = methodPath.startsWith("/");

        if (controllerEndsWithSeparator && methodStartsWithSeparator) {
            return controllerPath + methodPath.substring(1);
        } else if (controllerEndsWithSeparator || methodStartsWithSeparator) {
            return controllerPath + methodPath;
        }

        return controllerPath + "/" + methodPath;
    }

    private String trimEnding(String path, String ending) {
        return path.substring(0, path.length() - ending.length());
    }

    private String replaceAnyFileName(String controllerPath, String methodPath) {
        int methodDot = methodPath.indexOf(".");
        String methodFileName = methodDot == -1 ? methodPath : methodPath.substring(0, methodDot);
        String controllerExtension = controllerPath.substring(controllerPath.indexOf("*.") + 1);
        return methodFileName + controllerExtension;
    }

    private String directoryOf(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    private boolean containsPathVariable(String path) {
        return path.contains("{") || path.contains("}");
    }

    private boolean isFilePattern(String path) {
        return path.contains("*.") && !containsPathVariable(path) && !path.endsWith("/*") && !path.endsWith("/**");
    }

    private boolean hasSpecificExtension(String path) {
        return path.contains(".") && !path.endsWith(".*");
    }

    private boolean fitsInto(String path, String pattern) {
        String[] pathSegments = path.split("/", -1);
        String[] patternSegments = pattern.split("/", -1);
        boolean pathAndPatternHaveSameSegmentCount = pathSegments.length == patternSegments.length;
        if (!pathAndPatternHaveSameSegmentCount) {
            return false;
        }

        for (int i = 0; i < pathSegments.length; i++) {
            if (!segmentFits(pathSegments[i], patternSegments[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean segmentFits(String pathSegment, String patternSegment) {
        boolean segmentsAreEqual = pathSegment.equals(patternSegment);
        boolean patternSegmentIsWildcard = patternSegment.equals("*") || patternSegment.equals("**");
        boolean pathSegmentHasPatternSegmentExtension = patternSegment.startsWith("*.")
                && pathSegment.endsWith(patternSegment.substring(1));

        return segmentsAreEqual || patternSegmentIsWildcard || pathSegmentHasPatternSegmentExtension;
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

    private String mergeVersion(RequestMapping controllerMapping, RequestMapping methodMapping) {
        String controllerVersion = controllerMapping.getVersion();
        String methodVersion = methodMapping.getVersion();

        if (!methodVersion.isEmpty()) {
            return methodVersion;
        }

        return controllerVersion;
    }
}

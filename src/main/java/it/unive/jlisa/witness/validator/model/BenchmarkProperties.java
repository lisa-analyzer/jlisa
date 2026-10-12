package it.unive.jlisa.witness.validator.model;

import java.io.File;
import java.util.List;

public record BenchmarkProperties(List<File> inputFiles, File directory, boolean validAssert, boolean noRuntimeException) {
}

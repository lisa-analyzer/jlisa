package it.unive.jlisa.helpers;

import it.unive.lisa.outputs.compare.ResultComparer;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.outputs.json.JsonReport.JsonMessage;
import it.unive.lisa.outputs.json.JsonReport.JsonReproducingInput;

/**
 * A {@link ResultComparer} that ignores the values of the reproducing inputs
 * of warnings: several inputs might reproduce the same violation, and the
 * solver might find different ones in different executions. The sources,
 * locations, contexts and occurrences of the inputs are still compared.
 */
public class InputValuesIgnoringComparer extends ResultComparer {

	@Override
	public boolean compareWarnings(
			JsonReport first,
			JsonReport second) {
		// the reports are read from their files for each comparison, so
		// they can be modified
		forgetValues(first);
		forgetValues(second);
		return super.compareWarnings(first, second);
	}

	private static void forgetValues(
			JsonReport report) {
		for (JsonMessage warning : report.getWarnings())
			for (JsonReproducingInput input : warning.getReproducingInputs())
				input.setValue(null);
	}
}

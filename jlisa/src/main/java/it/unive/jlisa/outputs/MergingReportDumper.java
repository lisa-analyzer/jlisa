package it.unive.jlisa.outputs;

import it.unive.jlisa.program.JavaLocation;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.LiSAOutput;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/*
 * A JSON report dumper that eliminates duplicate warnings issued at locations that differ by
 * the copy number.
 * Loop unrolling can cause asserts to be duplicated. The analysis would raise multiple warnings
 * for the same assert (through the duplicates).
 *
 */
public class MergingReportDumper implements LiSAOutput {

	private final LiSAOutput delegate = new JSONReportDumper();

	@Override
	public void dump(Application app, LiSAReport report, ReportingTool tool, FileManager fileManager)
			throws IOException {
		LiSAReport merged = new LiSAReport(report.getConfiguration(),
				report.getRunInfo(),
				merge(report.getWarnings()),
				report.getNotices(),
				report.getCreatedFiles());
		delegate.dump(app, merged, tool, fileManager);
	}

	@Override
	public boolean isReportOutput() {
		return true;
	}

	static Collection<Message> merge(Collection<Message> warnings) {
		Map<String, StatementMessage> best = new TreeMap<>();
		Collection<Message> result = new ArrayList<>();

		for (Message m : warnings) {
			if (!(m instanceof StatementMessage sm)
			|| !(sm.getStatement().getLocation() instanceof JavaLocation jl)) {
				result.add(m);
				continue;
			}

			String key = sm.getStatement().getCFG() + "-" + jl.getCodeLocationNoCopy()
				+ "-" + sm.getStatement() + "-" + m.getMessage() + "-" + m.getContext();
			StatementMessage old = best.get(key);
			if (old == null || better(sm, old))
				best.put(key, sm);

		}
		result.addAll(best.values());
		return result;
	}

	static private boolean better(StatementMessage a, StatementMessage b) {
		if (a.isDefinite() != b.isDefinite())
			return a.isDefinite();
		return ((JavaLocation) a.getStatement().getLocation()).getCopy()
			< ((JavaLocation) b.getStatement().getLocation()).getCopy();
	}

}


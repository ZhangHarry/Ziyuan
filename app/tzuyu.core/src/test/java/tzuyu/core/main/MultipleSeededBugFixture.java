package tzuyu.core.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import faultLocalization.LineCoverageInfo;

public class MultipleSeededBugFixture extends SingleSeededBugFixture {
	// Map between an expected bug line and the found suspiciousness
	// (-1 if not found)
	private Map<String, Double> expectedBugLines = new HashMap<String, Double>();

	@Override
	public void expectedBugLine(final String line) {
		expectedBugLines.put(line, -1.0);
	}

	@Override
	protected void checkAnalyzedResults() {
		for (LineCoverageInfo info : report.getLineCoverageInfos()) {
			final Double value = expectedBugLines.get(info.getLocation().getId());
			if (value != null && value.doubleValue() < info.getSuspiciousness()) {
				expectedBugLines.put(info.getLocation().getId(), info.getSuspiciousness());
			}

			if (maxSuspiciousness < info.getSuspiciousness()) {
				maxSuspiciousness = info.getSuspiciousness();
			}
		}
	}

	@Override
	public boolean bugWasFound() {
		// At least 1 bug was found
		for (Double suspiciousness : expectedBugLines.values()) {
			if (suspiciousness > 0.0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean foundBugHasMaxSuspiciousness() {
		if (report.getLineCoverageInfos() == null || report.getLineCoverageInfos().size() == 0) {
			return false;
		}
		List<String> maxSuspiciousnessLines = new ArrayList<String>();
		for (LineCoverageInfo info : report.getLineCoverageInfos()) {
			if (info.getSuspiciousness() >= maxSuspiciousness) {
				maxSuspiciousnessLines.add(info.getLocation().getId());
			}
		}
		for (String codeLine : expectedBugLines.keySet()) {
			if (maxSuspiciousnessLines.contains(codeLine)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean foundBugIsInTopThree() {
		final double min = getSmallestSuspiciousnessInTopThree();
		for (Double suspiciousness : expectedBugLines.values()) {
			if (suspiciousness >= min) {
				return true;
			}
		}
		return false;
	}

	public String lineSuspiciousness() {
		final StringBuilder builder = new StringBuilder();
		for (Entry<String, Double> entry : expectedBugLines.entrySet()) {
			final String codeLine = entry.getKey();
			final double suspiciousness = entry.getValue().doubleValue();
			builder.append(codeLine).append(" > ").append(suspiciousness).append(LINE_FEED);
		}
		return builder.toString();
	}
}

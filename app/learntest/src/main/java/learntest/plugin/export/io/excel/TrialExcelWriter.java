package learntest.plugin.export.io.excel;

import static learntest.plugin.export.io.excel.TrialHeader.*;

import java.io.File;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import learntest.plugin.export.io.excel.common.ExcelWriter;

public class TrialExcelWriter extends ExcelWriter {
	private Sheet dataSheet;
	private int lastDataSheetRow;
	
	public TrialExcelWriter(File file) throws Exception {
		super(file);
	}

	@Override
	protected void initFromNewFile(File file) {
		super.initFromNewFile(file);
		lastDataSheetRow = TrialExcelConstants.DATA_SHEET_HEADER_ROW_IDX - 1;
		dataSheet = createSheet(TrialExcelConstants.DATA_SHEET_NAME);
		initDataSheetHeader();
	}
	
	@Override
	protected void initFromExistingFile(File file) throws Exception {
		super.initFromExistingFile(file);
		dataSheet = workbook.getSheet(TrialExcelConstants.DATA_SHEET_NAME);
		lastDataSheetRow = dataSheet.getLastRowNum();
	}
	
	private void initDataSheetHeader() {
		Row headerRow = newDataSheetRow();
		for (TrialHeader header : TrialHeader.values()) {
			addCell(headerRow, header, header.getTitle());
		}
	}

	public int addRowData(Trial trial) throws IOException {
		Row row = newDataSheetRow();
		addCell(row, METHOD_NAME, trial.getMethodName());
		
		if (trial.getJdartRtInfo() != null) {
			addCell(row, JDART_TIME, trial.getJdartRtInfo().getTime());
			addCell(row, JDART_COVERAGE, trial.getJdartRtInfo().getCoverage());
			addCell(row, JDART_TEST_CNT, trial.getJdartRtInfo().getTestCnt());
		}
		
		if (trial.getL2tRtInfo() != null) {
			addCell(row, L2T_TIME, trial.getL2tRtInfo().getTime());
			addCell(row, L2T_COVERAGE, trial.getL2tRtInfo().getCoverage());
			addCell(row, L2T_TEST_CNT, trial.getL2tRtInfo().getTestCnt());
		}
		
		if (trial.getRanRtInfo() != null) {
			addCell(row, RANDOOP_TIME, trial.getRanRtInfo().getTime());
			addCell(row, RANDOOP_COVERAGE, trial.getRanRtInfo().getCoverage());
			addCell(row, RANDOOP_TEST_CNT, trial.getRanRtInfo().getTestCnt());
		}
		
		addCell(row, ADVANTAGE, trial.getAdvantage());
		addCell(row, METHOD_LENGTH, trial.getMethodLength());
		addCell(row, METHOD_START_LINE, trial.getMethodStartLine());
		
		addCell(row, L2T_VALID_COVERAGE, trial.getL2tRtInfo().getValidCoverage());
		addCell(row, RANDOOP_VALID_COVERAGE, trial.getRanRtInfo().getValidCoverage());
		addCell(row, VALID_COVERAGE_ADV, trial.getL2tRtInfo().getValidCoverage()-trial.getRanRtInfo().getValidCoverage());
		
		if (trial instanceof MultiTrial) {
			MultiTrial multiTrial = (MultiTrial) trial;
			addCell(row, L2T_BEST_COVERAGE, multiTrial.getBestL2tRtCoverage());
			addCell(row, RANDOOP_BEST_COVERAGE, multiTrial.getBestRanRtCoverage());
			addCell(row, VALID_NUM, multiTrial.getValidNum());
		}
		writeWorkbook();
		return lastDataSheetRow;
	}

	private Row newDataSheetRow() {
		return dataSheet.createRow(++lastDataSheetRow);
	}
	
}

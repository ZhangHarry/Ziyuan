package learntest.local.explore.basic;
import static learntest.local.explore.basic.TrialHeader.*;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import learntest.core.commons.exception.LearnTestException;
import learntest.plugin.export.io.excel.TrialExcelConstants;
import learntest.plugin.export.io.excel.common.ExcelReader;
import learntest.plugin.export.io.excel.common.ExcelSettings;
import sav.common.core.utils.Assert;

/**
 * @author ZhangHr
 */
public class DetailExcelReader extends ExcelReader {

	private Sheet dataSheet;

	public DetailExcelReader(File file) throws Exception {
		super(file);
	}

	@Override
	public void reset(File file) throws Exception {
		super.reset(file);
		dataSheet = workbook.getSheet(TrialExcelConstants.DATA_SHEET_NAME);
		if (dataSheet == null) {
			throw new LearnTestException("invalid experimental file! (Cannot get data sheet)");
		}
	}

	public List<MethodTrial> readDataSheet() {
		Assert.assertNotNull(dataSheet, "TrialExcelReader has not initialized!");
		Iterator<Row> it = dataSheet.rowIterator();
		Row header = it.next(); // ignore first row (header)
		Assert.assertTrue(isDataSheetHeader(header), "Data sheet is invalid!");
		List<MethodTrial> data = new LinkedList<>();
		try {
			while (it.hasNext()) {
				Row row = it.next();
				readDataSheetRow(row, data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	public boolean hasValidHeader() {
		Row header = dataSheet.iterator().next();
		return isDataSheetHeader(header);
	}

	private void readDataSheetRow(Row row, List<MethodTrial> data) {
		MethodTrial trial = new MethodTrial();
		trial.setMethodName(getStringCellValue(row, TrialHeader.METHOD_NAME));
		trial.setLine(getIntCellValue(row, TrialHeader.METHOD_START_LINE));
		trial.setJdartCnt(getIntCellValue(row, TrialHeader.JDART_TEST_CNT));
		trial.setJdartTime(getIntCellValue(row, TrialHeader.JDART_TIME));
		trial.setJdartCov(getDoubleCellValue(row, TrialHeader.JDART_COVERAGE));
		trial.setEvosuiteCov(getDoubleCellValue(row, EVOSUITECOV));
		trial.setEvosuiteInfo(getStringCellValue(row, EVOSUITEINFO));
		trial.setL2tTime(getIntCellValue(row, TrialHeader.L2T_TIME));
		trial.setRandoopTime(getIntCellValue(row, TrialHeader.RANDOOP_TIME));
		
		double l2tMaxCov = 0;
		double randoopMaxCov = 0;
		
		for (int i = 0; i < 5; i++) {
			DetailTrial dTrial = new DetailTrial();
			dTrial.setLine(trial.getLine());
			dTrial.setMethodName(trial.getMethodName());
			dTrial.setL2tCostTime(trial.getL2tTime());
			dTrial.setRandCostTime(trial.getRandoopTime());
			switch (i) {
			case 0:
				if (row.getCell(FIRST_TRIAL_ADV.getCellIdx()) != null) {
					dTrial.setLearnedState(getIntCellValue(row, FIRST_TRIAL_L));
					dTrial.setAdvantage(getDoubleCellValue(row, FIRST_TRIAL_ADV));
					dTrial.setL2t(getDoubleCellValue(row, FIRST_TRIAL_L2T));
					dTrial.setRandoop(getDoubleCellValue(row, FIRST_TRIAL_R));
					dTrial.setJdart(getDoubleCellValue(row, FIRST_TRIAL_JDART));
					dTrial.setL2tBetter(getStringCellValue(row, FIRST_RAND_WORSE_THAN_L2T));
					dTrial.setRanBetter(getStringCellValue(row, FIRST_L2T_WORSE_THAN_RAND));
					dTrial.setJdartSolveTimes(getIntCellValue(row, FIRST_TRIAL_JDART_SOLVE_TIMES));
					dTrial.setL2tSolveTimes(getIntCellValue(row, FIRST_SYMBOLIC_TIMES));
					trial.getTrials().add(dTrial);
					if (dTrial.getL2t() > l2tMaxCov) {
						l2tMaxCov = dTrial.getL2t();
					}
					if (dTrial.getRandoop() > randoopMaxCov) {
						randoopMaxCov = dTrial.getRandoop();
					}
//					String timeLine = getStringCellValue(row, FIRST_COV_TIMELINE);
//					String[] strings = timeLine.split(";");
//					dTrial.setL2tTimeLine(strings[0]);
//					dTrial.setRandTimeLine(strings[1]);
				}
				break;
			case 1:
				if (row.getCell(SECOND_TRIAL_ADV.getCellIdx()) != null) {
					dTrial.setLearnedState(getIntCellValue(row, SECOND_TRIAL_L));
					dTrial.setAdvantage(getDoubleCellValue(row, SECOND_TRIAL_ADV));
					dTrial.setL2t(getDoubleCellValue(row, SECOND_TRIAL_L2T));
					dTrial.setRandoop(getDoubleCellValue(row, SECOND_TRIAL_R));
					dTrial.setJdart(getDoubleCellValue(row, SECOND_TRIAL_JDART));
					dTrial.setL2tBetter(getStringCellValue(row, SECOND_RAND_WORSE_THAN_L2T));
					dTrial.setRanBetter(getStringCellValue(row, SECOND_L2T_WORSE_THAN_RAND));
					dTrial.setJdartSolveTimes(getIntCellValue(row, SECOND_TRIAL_JDART_SOLVE_TIMES));
					dTrial.setL2tSolveTimes(getIntCellValue(row, SECOND_SYMBOLIC_TIMES));
					trial.getTrials().add(dTrial);
					if (dTrial.getL2t() > l2tMaxCov) {
						l2tMaxCov = dTrial.getL2t();
					}
					if (dTrial.getRandoop() > randoopMaxCov) {
						randoopMaxCov = dTrial.getRandoop();
					}
//					String timeLine = getStringCellValue(row, SECOND_COV_TIMELINE);
//					String[] strings = timeLine.split(";");
//					dTrial.setL2tTimeLine(strings[0]);
//					dTrial.setRandTimeLine(strings[1]);
				}
				break;
			case 2:
				if (row.getCell(THIRD_TRIAL_ADV.getCellIdx()) != null) {
					dTrial.setLearnedState(getIntCellValue(row, THIRD_TRIAL_L));
					dTrial.setAdvantage(getDoubleCellValue(row, THIRD_TRIAL_ADV));
					dTrial.setL2t(getDoubleCellValue(row, THIRD_TRIAL_L2T));
					dTrial.setRandoop(getDoubleCellValue(row, THIRD_TRIAL_R));
					dTrial.setJdart(getDoubleCellValue(row, THIRD_TRIAL_JDART));
					dTrial.setL2tBetter(getStringCellValue(row, THIRD_RAND_WORSE_THAN_L2T));
					dTrial.setRanBetter(getStringCellValue(row, THIRD_L2T_WORSE_THAN_RAND));
					dTrial.setJdartSolveTimes(getIntCellValue(row, THIRD_TRIAL_JDART_SOLVE_TIMES));
					dTrial.setL2tSolveTimes(getIntCellValue(row, THIRD_SYMBOLIC_TIMES));
					trial.getTrials().add(dTrial);
					if (dTrial.getL2t() > l2tMaxCov) {
						l2tMaxCov = dTrial.getL2t();
					}
					if (dTrial.getRandoop() > randoopMaxCov) {
						randoopMaxCov = dTrial.getRandoop();
					}
//					String timeLine = getStringCellValue(row, THIRD_COV_TIMELINE);
//					String[] strings = timeLine.split(";");
//					dTrial.setL2tTimeLine(strings[0]);
//					dTrial.setRandTimeLine(strings[1]);
				}
				break;
			case 3:
				if (row.getCell(FORTH_TRIAL_ADV.getCellIdx()) != null) {
					dTrial.setLearnedState(getIntCellValue(row, FORTH_TRIAL_L));
					dTrial.setAdvantage(getDoubleCellValue(row, FORTH_TRIAL_ADV));
					dTrial.setL2t(getDoubleCellValue(row, FORTH_TRIAL_L2T));
					dTrial.setRandoop(getDoubleCellValue(row, FORTH_TRIAL_R));
					dTrial.setJdart(getDoubleCellValue(row, FORTH_TRIAL_JDART));
					dTrial.setL2tBetter(getStringCellValue(row, FORTH_RAND_WORSE_THAN_L2T));
					dTrial.setRanBetter(getStringCellValue(row, FORTH_L2T_WORSE_THAN_RAND));
					dTrial.setJdartSolveTimes(getIntCellValue(row, FORTH_TRIAL_JDART_SOLVE_TIMES));
					dTrial.setL2tSolveTimes(getIntCellValue(row, FORTH_SYMBOLIC_TIMES));
					trial.getTrials().add(dTrial);
					if (dTrial.getL2t() > l2tMaxCov) {
						l2tMaxCov = dTrial.getL2t();
					}
					if (dTrial.getRandoop() > randoopMaxCov) {
						randoopMaxCov = dTrial.getRandoop();
					}
//					String timeLine = getStringCellValue(row, FORTH_COV_TIMELINE);
//					String[] strings = timeLine.split(";");
//					dTrial.setL2tTimeLine(strings[0]);
//					dTrial.setRandTimeLine(strings[1]);
				}
				break;
			case 4:
				if (row.getCell(FIFTH_TRIAL_ADV.getCellIdx()) != null) {
					dTrial.setLearnedState(getIntCellValue(row, FIFTH_TRIAL_L));
					dTrial.setAdvantage(getDoubleCellValue(row, FIFTH_TRIAL_ADV));
					dTrial.setL2t(getDoubleCellValue(row, FIFTH_TRIAL_L2T));
					dTrial.setRandoop(getDoubleCellValue(row, FIFTH_TRIAL_R));
					dTrial.setJdart(getDoubleCellValue(row, FIFTH_TRIAL_JDART));
					dTrial.setL2tBetter(getStringCellValue(row, FIFTH_RAND_WORSE_THAN_L2T));
					dTrial.setRanBetter(getStringCellValue(row, FIFTH_L2T_WORSE_THAN_RAND));
					dTrial.setJdartSolveTimes(getIntCellValue(row, FIFTH_TRIAL_JDART_SOLVE_TIMES));
					dTrial.setL2tSolveTimes(getIntCellValue(row,FIFTH_SYMBOLIC_TIMES));
					trial.getTrials().add(dTrial);
					if (dTrial.getL2t() > l2tMaxCov) {
						l2tMaxCov = dTrial.getL2t();
					}
					if (dTrial.getRandoop() > randoopMaxCov) {
						randoopMaxCov = dTrial.getRandoop();
					}
//					String timeLine = getStringCellValue(row, FIFTH_COV_TIMELINE);
//					String[] strings = timeLine.split(";");
//					dTrial.setL2tTimeLine(strings[0]);
//					dTrial.setRandTimeLine(strings[1]);
				}
				break;

			default:
				break;
			}
		}
		setValidAveCoverageAdv(trial);
		trial.setL2tMaxCov(l2tMaxCov);
		trial.setRandoopMaxCov(randoopMaxCov);
		data.add(trial);

	}

	private void setValidAveCoverageAdv(MethodTrial trial) {

		double valid_cov_l = 0;
		double valid_cov_r = 0;
		int validNum = 0;
		List<DetailTrial> trials = trial.getTrials();
		for (DetailTrial detailTrial : trials) {
			if (detailTrial.getLearnedState() > 0) {
				validNum++;
				valid_cov_l += detailTrial.getL2t();
				valid_cov_r += detailTrial.getRandoop();
			}
		}
		double valid_adv_cov = validNum > 0 ? (valid_cov_l-valid_cov_r)/validNum : 0;
		trial.setValidAveCoverageAdv(valid_adv_cov);
		
	}

	private boolean isDataSheetHeader(Row header) {
		if (header.getRowNum() != ExcelSettings.DATA_SHEET_HEADER_ROW_IDX) {
			return false;
		}
		for (TrialHeader title : TrialHeader.values()) {
			if (!title.getTitle().equals(header.getCell(title.getCellIdx()).getStringCellValue())) {
				return false;
			}
		}
		return true;
	}

	public int getLastDataSheetRow() {
		return dataSheet.getLastRowNum();
	}
}

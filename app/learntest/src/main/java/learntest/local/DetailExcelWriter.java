package learntest.local;

import static learntest.plugin.export.io.excel.TrialHeader.ADVANTAGE;
import static learntest.plugin.export.io.excel.TrialHeader.AVE_COVERAGE_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_L2T_WORSE_THAN_RAND;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_RAND_WORSE_THAN_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_TRIAL_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_TRIAL_L;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_TRIAL_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FIFTH_TRIAL_R;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_L2T_WORSE_THAN_RAND;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_RAND_WORSE_THAN_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_TRIAL_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_TRIAL_L;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_TRIAL_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FIRST_TRIAL_R;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_L2T_WORSE_THAN_RAND;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_RAND_WORSE_THAN_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_TRIAL_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_TRIAL_L;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_TRIAL_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.FORTH_TRIAL_R;
import static learntest.plugin.export.io.excel.TrialHeader.JDART_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.JDART_TEST_CNT;
import static learntest.plugin.export.io.excel.TrialHeader.JDART_TIME;
import static learntest.plugin.export.io.excel.TrialHeader.L2T_BEST_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.L2T_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.L2T_TEST_CNT;
import static learntest.plugin.export.io.excel.TrialHeader.L2T_TIME;
import static learntest.plugin.export.io.excel.TrialHeader.L2T_VALID_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.METHOD_LENGTH;
import static learntest.plugin.export.io.excel.TrialHeader.METHOD_NAME;
import static learntest.plugin.export.io.excel.TrialHeader.METHOD_START_LINE;
import static learntest.plugin.export.io.excel.TrialHeader.RANDOOP_BEST_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.RANDOOP_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.RANDOOP_TEST_CNT;
import static learntest.plugin.export.io.excel.TrialHeader.RANDOOP_TIME;
import static learntest.plugin.export.io.excel.TrialHeader.RANDOOP_VALID_COVERAGE;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_L2T_WORSE_THAN_RAND;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_RAND_WORSE_THAN_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_TRIAL_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_TRIAL_L;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_TRIAL_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.SECOND_TRIAL_R;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_L2T_WORSE_THAN_RAND;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_RAND_WORSE_THAN_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_TRIAL_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_TRIAL_L;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_TRIAL_L2T;
import static learntest.plugin.export.io.excel.TrialHeader.THIRD_TRIAL_R;
import static learntest.plugin.export.io.excel.TrialHeader.VALID_COVERAGE_ADV;
import static learntest.plugin.export.io.excel.TrialHeader.VALID_NUM;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;

import learntest.plugin.export.io.excel.common.SimpleExcelWriter;

public class DetailExcelWriter extends SimpleExcelWriter<MethodTrial> {

	public DetailExcelWriter(File file) throws Exception {
		super(file);
	}

	protected void addRowData(Row row, MethodTrial trial) throws IOException {
		addCell(row, METHOD_NAME, trial.getMethodName());
		addCell(row, METHOD_START_LINE, trial.getLine());
		addCell(row, JDART_TIME, trial.getJdartTime());
		addCell(row, JDART_COVERAGE, trial.getJdartCov());
		addCell(row, JDART_TEST_CNT, trial.getJdartCnt());
		addCell(row, VALID_COVERAGE_ADV, trial.getValidAveCoverageAdv());

		export5Trials(row, trial.getTrials());
		writeWorkbook();
	}

	private void export5Trials(Row row, List<DetailTrial> trials) {
		for (int i = 0; i < trials.size() && i < 5; i++) {
			DetailTrial trial = trials.get(i);
			switch (i) {
			case 0:
				addCell(row, FIRST_TRIAL_R, trial.getRandoop());
				addCell(row, FIRST_TRIAL_L2T, trial.getL2t());
				addCell(row, FIRST_TRIAL_L, trial.getLearnedState());
				addCell(row, FIRST_TRIAL_ADV, trial.getL2t() - trial.getRandoop());
				addCell(row, FIRST_RAND_WORSE_THAN_L2T, trial.getL2tBetter());
				addCell(row, FIRST_L2T_WORSE_THAN_RAND, trial.getRanBetter());
				break;
			case 1:
				addCell(row, SECOND_TRIAL_R, trial.getRandoop());
				addCell(row, SECOND_TRIAL_L2T, trial.getL2t());
				addCell(row, SECOND_TRIAL_L, trial.getLearnedState());
				addCell(row, SECOND_TRIAL_ADV, trial.getL2t() - trial.getRandoop());
				addCell(row, SECOND_RAND_WORSE_THAN_L2T, trial.getL2tBetter());
				addCell(row, SECOND_L2T_WORSE_THAN_RAND, trial.getRanBetter());
				break;
			case 2:
				addCell(row, THIRD_TRIAL_R, trial.getRandoop());
				addCell(row, THIRD_TRIAL_L2T, trial.getL2t());
				addCell(row, THIRD_TRIAL_L, trial.getLearnedState());
				addCell(row, THIRD_TRIAL_ADV, trial.getL2t() - trial.getRandoop());
				addCell(row, THIRD_RAND_WORSE_THAN_L2T, trial.getL2tBetter());
				addCell(row, THIRD_L2T_WORSE_THAN_RAND, trial.getRanBetter());
				break;
			case 3:
				addCell(row, FORTH_TRIAL_R, trial.getRandoop());
				addCell(row, FORTH_TRIAL_L2T, trial.getL2t());
				addCell(row, FORTH_TRIAL_L, trial.getLearnedState());
				addCell(row, FORTH_TRIAL_ADV, trial.getL2t() - trial.getRandoop());
				addCell(row, FORTH_RAND_WORSE_THAN_L2T, trial.getL2tBetter());
				addCell(row, FORTH_L2T_WORSE_THAN_RAND, trial.getRanBetter());
				break;
			case 4:
				addCell(row, FIFTH_TRIAL_R, trial.getRandoop());
				addCell(row, FIFTH_TRIAL_L2T, trial.getL2t());
				addCell(row, FIFTH_TRIAL_L, trial.getLearnedState());
				addCell(row, FIFTH_TRIAL_ADV, trial.getL2t() - trial.getRandoop());
				addCell(row, FIFTH_RAND_WORSE_THAN_L2T, trial.getL2tBetter());
				addCell(row, FIFTH_L2T_WORSE_THAN_RAND, trial.getRanBetter());
				break;

			default:
				break;
			}
		}

	}

}

/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.commons.test.gan.eval102;

import static learntest.core.commons.test.gan.eval102.GanTestExcelWriter102.Header.*;

import java.io.File;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;

import learntest.core.commons.test.gan.eval102.NodeExportData102.BranchTraningData;
import learntest.plugin.export.io.excel.common.ExcelHeader;
import learntest.plugin.export.io.excel.common.SimpleExcelWriter;

/**
 * @author LLT following the format as in the issue #102
 *         https://github.com/sunjun-group/Ziyuan/issues/102
 */
public class GanTestExcelWriter102 extends SimpleExcelWriter<RowData102> {

	public GanTestExcelWriter102(File file) throws Exception {
		super(file, Header.values());
	}

	public void export(RowData102 data) throws IOException {
		if (data.getRowNum() < 0) {
			addRowData(data);
		} else {
			addRowData(getRow(data.getRowNum()), data);
		}
		writeWorkbook();
	}

	@Override
	protected void addRowData(Row row, RowData102 rowData) throws IOException {
		NodeExportData102 data = rowData.getNodeData();
		addCell(row, METHOD_NAME, data.getMethodId());
		addCell(row, NODE_ID, data.getNodeIdx());
		addCell(row, LINE_NUMBER, data.getLineNum());
		addBranchTrainingData(data.getTrueTrainData(), row, 0);
		addBranchTrainingData(data.getTrueTrainData(), row, 1);
	}

	private void addBranchTrainingData(BranchTraningData data, Row row, int offset) {
		addCell(row, Header.valueOf(TRUE_BRANCH_TRAIN_DPS_TOTAL, offset), data.getTrainDpsTotal());
		addCell(row, Header.valueOf(TRUE_BRANCH_GEN_DPS_TOTAL, offset), data.getGenDpsTotal());
		addCell(row, Header.valueOf(TRUE_BRANCH_GEN_ACC, offset), data.getAvgAcc());
		addCell(row, Header.valueOf(TRUE_BRANCH_TRAIN_DPS, offset), data.getTrainDpsStr());
		addCell(row, Header.valueOf(TRUE_BRANCH_CORRECT_GEN_DPS, offset), data.getCorrectGenDpsStr());
		addCell(row, Header.valueOf(TRUE_BRANCH_WRONG_GEN_DPS, offset), data.getWrongGenDpsStr());
	}

	public static enum Header implements ExcelHeader {
		METHOD_NAME ("method name"),
		NODE_ID ("node_id"),
		LINE_NUMBER ("line_number"),
		
		TRUE_BRANCH_TRAIN_DPS_TOTAL("number of traning data points for TRUE branch"),
		FALSE_BRANCH_TRAIN_DPS_TOTAL("number of traning data points for FALSE branch"),
		
		TRUE_BRANCH_GEN_DPS_TOTAL("number of genrated data points for TRUE branch"),
		FALSE_BRANCH_GEN_DPS_TOTAL("number of generated data points for FALSE branch"),
		
		TRUE_BRANCH_GEN_ACC("accuracy of generated data points for TRUE branch"),
		FALSE_BRANCH_GEN_ACC("accuracy of generated data points for FALSE branch"),
		
		TRUE_BRANCH_TRAIN_DPS("training data points for TRUE branch"),
		FALSE_BRANCH_TRAIN_DPS("training data points for FALSE branch"),
		
		TRUE_BRANCH_CORRECT_GEN_DPS("correctly generated data points for TRUE branch"),
		FALSE_BRANCH_CORRECT_GEN_DPS("correctly generated data points for FALSE branch"),
		
		TRUE_BRANCH_WRONG_GEN_DPS("wrongly generated data points for TRUE branch"),
		FALSE_BRANCH_WRONG_GEN_DPS("wrongly generated data points for FALSE branch");

		private String title;
		private Header(String title) {
			this.title = title;
		}

		public static ExcelHeader valueOf(Header header, int offset) {
			return values()[header.getCellIdx() + offset];
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public int getCellIdx() {
			return ordinal();
		}

	}
}

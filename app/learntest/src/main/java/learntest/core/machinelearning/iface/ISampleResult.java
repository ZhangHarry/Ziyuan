/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.machinelearning.iface;

import learntest.core.commons.data.decision.IDecisionNode;
import learntest.testcase.data.INodeCoveredData;

/**
 * @author LLT
 *
 */
public interface ISampleResult {

	INodeCoveredData getNewData(IDecisionNode target);

}

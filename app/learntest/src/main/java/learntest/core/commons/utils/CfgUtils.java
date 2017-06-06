/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.commons.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfgcoverage.jacoco.analysis.data.BranchRelationship;
import cfgcoverage.jacoco.analysis.data.CFG;
import cfgcoverage.jacoco.analysis.data.CfgNode;
import cfgcoverage.jacoco.utils.CfgConstructorUtils;
import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 *
 */
public class CfgUtils {
	private CfgUtils() {}
	
	public static CfgNode getVeryFirstDecisionNode(CFG cfg) {
		return CfgConstructorUtils.getVeryFirstDecisionNode(cfg.getDecisionNodes());
	}

	@SuppressWarnings("unchecked")
	public static Collection<CfgNode> getPrecondInherentDominatee(CfgNode node) {
		if (node.getDominatees() == null) {
			return Collections.EMPTY_LIST;
		}
		List<CfgNode> result = new ArrayList<CfgNode>(node.getDominatees().size());
		for (CfgNode dominatee : CollectionUtils.nullToEmpty(node.getDominatees())) {
			BranchRelationship branchRelationship = node.getBranchRelationship(dominatee.getIdx());
			if (branchRelationship != BranchRelationship.TRUE_FALSE) {
				result.add(dominatee);
			}
		}
		return result;
	}
}

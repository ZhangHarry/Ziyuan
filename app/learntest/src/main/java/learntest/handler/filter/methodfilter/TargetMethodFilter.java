/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.handler.filter.methodfilter;

import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * @author LLT
 *
 */
public interface TargetMethodFilter {

	boolean isValid(MethodDeclaration md);

}

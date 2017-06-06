/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.handler.filter.classfilter;

import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import sav.common.core.utils.ClassUtils;

/**
 * @author LLT
 *
 */
public class ClassNameFilter implements TargetClassFilter {
	private List<String> excludedClasses;
	
	public ClassNameFilter(List<String> excludedClasses) {
		this.excludedClasses = excludedClasses;
	}

	public boolean isValid(CompilationUnit cu) {
		if (cu.types().isEmpty()) {
			return false;
		}
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) cu.types().get(0);
		if (excludedClasses.contains(ClassUtils.getCanonicalName(cu.getPackage().getName().getFullyQualifiedName(),
				type.getName().getFullyQualifiedName()))) {
			return false;
		}
		return true;
	}
}

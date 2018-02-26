/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.plugin.handler.filter.classfilter;

import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import sav.common.core.utils.ClassUtils;

/**
 * @author LLT
 *
 */
public class ClassNameFilter implements ITypeFilter {
	boolean filterKind; // if TRUE, only reserve specialClasses, otherwise only discard specialClasses
	private List<String> specialClasses;
	
	public ClassNameFilter(List<String> specialClasses, boolean filterKind) {
		this.specialClasses = specialClasses;
		this.filterKind = filterKind;
	}

	public boolean isValid(CompilationUnit cu) {
		if (cu.types().isEmpty()) {
			return false;
		}
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) cu.types().get(0);
		if (specialClasses.contains(ClassUtils.getCanonicalName(cu.getPackage().getName().getFullyQualifiedName(),
				type.getName().getFullyQualifiedName()))) {
			return filterKind;
		}
		return !filterKind;
	}

	public boolean isValid(TypeDeclaration typeDecl) {
		return true;
	}
}

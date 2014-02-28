/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import tzuyu.engine.iface.IReferencesAnalyzer;
import tzuyu.engine.model.exception.TzuyuException;
import tzuyu.engine.utils.CollectionUtils;
import tzuyu.engine.utils.Randomness;
import tzuyu.plugin.core.exception.PluginException;
import tzuyu.plugin.core.utils.ClassLoaderUtils;
import tzuyu.plugin.core.utils.IResourceUtils;
import tzuyu.plugin.reporter.PluginLogger;

/**
 * @author LLT
 * 
 */
public class PluginReferencesAnalyzer implements IReferencesAnalyzer {
	// add this into the configuration
	private int numOfRandomClzzToCache = 10;
	private IJavaProject project;
	private Map<Class<?>, List<IType>> itypeCache;

	public PluginReferencesAnalyzer(IJavaProject project) {
		itypeCache = new HashMap<Class<?>, List<IType>>();
		this.project = project;
	}

	@Override
	public Class<?> getRandomClass() {
		IType type;
		List<IType> itypes = itypeCache.get(Class.class);
		if (CollectionUtils.isEmpty(itypes)) {
			itypes = new ArrayList<IType>();
		}
		if (itypes.size() < numOfRandomClzzToCache) {
			type = getRandomClassFromProject(project);
		} else {
			type = Randomness.randomMember(itypes);
		}
		try {
			return toClass(type);
		} catch (TzuyuException e) {
			return null;
		}
	}

	@Override
	public Class<?> getRandomEnum() {
		List<IType> enums = itypeCache.get(Enum.class);
		if (CollectionUtils.isEmpty(enums)) {
			enums = new ArrayList<IType>();
			itypeCache.put(Enum.class, enums);
		}
		try {
			List<String> enumTypes = performSearchEnums(project, numOfRandomClzzToCache);
			for (String eString : enumTypes) {
				CollectionUtils.addIfNotNull(enums, project.findType(eString));
			}
		} catch (CoreException e) {
			PluginLogger.logEx(e);
			return null;
		}
		try {
			return toClass(Randomness.randomMember(enums));
		} catch (TzuyuException e) {
			return null;
		}
	}

	/**
	 * using eclipse API to search enums of the project
	 * limited by the limit number.
	 * Because eclipse perform the searching on the project source first, 
	 * then on the binary sources.
	 * So, we should limit the size of result list for the performance. 
	 */
	private List<String> performSearchEnums(IJavaProject proj, final int limit) {
		final List<String> result = new ArrayList<String>();
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[]{proj});
		// for cancel the the search perform when we found enough enum for test.
		final IProgressMonitor progressMonitor = new NullProgressMonitor();  
		TypeNameRequestor nameRequestor = new TypeNameRequestor() {
			@Override
			public void acceptType(int modifiers, char[] packageName,
					char[] simpleTypeName, char[][] enclosingTypeNames,
					String path) {
				String className = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
				if (className.equals(String.copyValueOf(simpleTypeName))) {
					result.add(new StringBuilder().append(packageName).append(".")
									.append(className).toString());
				}
				if (result.size() == limit) {
					progressMonitor.setCanceled(true);
				}
			}
		};
		int matchRule = SearchPattern.R_PREFIX_MATCH;
		SearchEngine searchEngine = new SearchEngine();
		try {
			searchEngine.searchAllTypeNames(new char[]{}, matchRule, new char[]{},
					matchRule, IJavaSearchConstants.ENUM, scope, nameRequestor, 
					IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
					progressMonitor);
			return result;
		} catch (OperationCanceledException e) {
			// do nothing
			return result;
		} catch (JavaModelException e) {
			PluginLogger.logEx(e);
			return new ArrayList<String>();
		}
	}

	@Override
	public Class<?> getRandomImplClzz(Class<?> clazz) {
		try {
			if (!clazz.isInterface() && clazz != Enum.class) {
				return clazz;
			}
			// for interface, using eclipse utils to get its implemetation.
			List<IType> availableSubTypes = itypeCache.get(clazz);
			if (availableSubTypes == null) {
				IType type = getIType(project, clazz);
				IType[] allSubtypes;
				try {
					allSubtypes = getAllSubtypes(project, type);
					availableSubTypes = getRandomSublist(allSubtypes);
				} catch (JavaModelException e) {
					PluginLogger.logEx(e);
				}
				itypeCache.put(clazz, availableSubTypes);
			}

			IType subType = Randomness.randomMember(availableSubTypes);
			System.out.println("Randomness. selected for interface: "
					+ clazz.getName() + "\n Result: "
					+ (subType == null ? "" : subType.getElementName()));
			return toClass(subType);
		} catch (TzuyuException e) {
			return null;
		}
	}

	private Class<?> toClass(IType type) throws TzuyuException {
		try {
			if (type == null) {
				return null;
			}
			return ClassLoaderUtils.getClassLoader(project).loadClass(
					type.getFullyQualifiedName());
		} catch (ClassNotFoundException e) {
			PluginLogger.logEx(e);
			TzuyuException.rethrow(e);
		} catch (PluginException e) {
			PluginLogger.logEx(e);
			TzuyuException.rethrow(e);
		}
		return null;
	}

	private IType getRandomClassFromProject(IJavaProject proj) {
		try {
			IPackageFragment pkg = Randomness.randomMember(IResourceUtils
					.filterSourcePkgs(proj.getPackageFragments()));
			ICompilationUnit cu = Randomness.randomMember(pkg
					.getCompilationUnits());
			if (cu == null) {
				return null;
			}
			IType type = CollectionUtils.getFirstElement(cu.getTypes());
			return type;
		} catch (JavaModelException e) {
			PluginLogger.logEx(e);
			return null;
		}
	}

	private List<IType> getRandomSublist(IType[] allSubtypes) {
		List<IType> allList = Arrays.asList(allSubtypes);
		return getRandomSubList(allList, numOfRandomClzzToCache);
	}

	private List<IType> getRandomSubList(List<IType> allList, int sublistMaxSize) {
		if (CollectionUtils.isEmpty(allList)) {
			return new ArrayList<IType>();
		}
		int size = sublistMaxSize;
		if (size >= allList.size()) {
			size = allList.size();
		}
		List<IType> result = new ArrayList<IType>();
		Collections.shuffle(allList);
		int i = 0;
		for (Iterator<IType> it = allList.iterator(); it.hasNext() && i < size;) {
			IType iType = it.next();
			try {
				if (!iType.isAnonymous() && !iType.isInterface()
						&& !iType.isMember()) {
					result.add(iType);
					PluginLogger.log(iType.getElementName());
					i++;
				}
			} catch (JavaModelException e) {
				PluginLogger.logEx(e);
			}

		}
		return result;
	}

	private static IType[] getAllSubtypes(IJavaProject project, IType type)
			throws JavaModelException {
		IType[] allSubtypes = type.newTypeHierarchy(project, null)
				.getAllSubtypes(type);
		return allSubtypes;
	}

	private static IType getIType(IJavaProject project, Class<?> clazz) {
		try {
			return project.findType(clazz.getName());
		} catch (JavaModelException e) {
			PluginLogger.logEx(e);
			return null;
		}
	}
}

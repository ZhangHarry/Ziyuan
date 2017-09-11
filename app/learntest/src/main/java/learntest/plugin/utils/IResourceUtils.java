/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.plugin.utils;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IJavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import learntest.plugin.commons.LearntestPluginExeception;

/**
 * @author LLT
 *
 */
public class IResourceUtils {
	private static Logger log = LoggerFactory.getLogger(IResourceUtils.class);
	private IResourceUtils(){}
	
	public static void getTestTarget(IJavaProject project) {
	}
	
	public static String getResourceAbsolutePath(String pluginId, String resourceRelativePath)
			throws LearntestPluginExeception {
		try {
			String resourceUrl = getResourceUrl(pluginId, resourceRelativePath);
			URL fileURL = new URL(resourceUrl);
			URL resolve = FileLocator.resolve(fileURL);
			URI uri = resolve.toURI();
			File file = new File(uri);
			return file.getAbsolutePath();
		} catch (Exception e1) {
			log.error(e1.getMessage());
			throw new LearntestPluginExeception(e1);
		}
	}

	private static String getResourceUrl(String pluginId, String resourceRelativePath) {
		StringBuilder sb = new StringBuilder("platform:/plugin/").append(pluginId).append("/")
				.append(resourceRelativePath);
		return sb.toString();
	}
	
}

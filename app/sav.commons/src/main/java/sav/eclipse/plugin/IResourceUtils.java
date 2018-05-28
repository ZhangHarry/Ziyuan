/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.eclipse.plugin;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			throws PluginException {
		try {
			String resourceUrl = getResourceUrl(pluginId, resourceRelativePath);
			URL fileURL = new URL(resourceUrl);
			URL resolve = FileLocator.resolve(fileURL);
			URI uri = resolve.toURI();
			File file = new File(uri);
			return file.getAbsolutePath();
		} catch (Exception e1) {
			log.error(e1.getMessage());
			throw new PluginException(e1);
		}
	}

	private static String getResourceUrl(String pluginId, String resourceRelativePath) {
		StringBuilder sb = new StringBuilder("platform:/plugin/").append(pluginId).append("/")
				.append(resourceRelativePath);
		return sb.toString();
	}
	
    public static IPath relativeToAbsolute(IPath relativePath) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(relativePath);
        if (resource != null) {
            return resource.getLocation();
        }
        return relativePath;
    }
}

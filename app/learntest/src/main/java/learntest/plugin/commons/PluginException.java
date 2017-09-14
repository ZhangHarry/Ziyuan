/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.plugin.commons;

import org.eclipse.jdt.core.JavaModelException;

/**
 * @author LLT
 *
 */
public class PluginException extends Exception {
	private static final long serialVersionUID = 1L;

	public PluginException(String message) {
		super(message);
	}
	
	public PluginException(Exception e) {
		super(e);
	}

	public static PluginException wrapEx(JavaModelException e) {
		return new PluginException(e.getMessage());
	}

}
